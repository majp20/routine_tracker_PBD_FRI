package si.uni_lj.fri.pbd.routinetracker.ui.main
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import data.RoutinesDatabase
import data.dao.RoutineListDisplay
import data.entity.Routine
import kotlinx.coroutines.launch
import repository.RoutineRepository
import si.uni_lj.fri.pbd.routinetracker.R
import util.RoutineEvaluationWorker
import viewmodel.RoutineListViewModel
import viewmodel.RoutineListViewModelFactory

class RoutineListFragment : Fragment() {

    private lateinit var routineListVM: RoutineListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = RoutinesDatabase.getDatabase(requireContext()).routineDao()
        val repository = RoutineRepository(dao, requireContext().applicationContext)
        val factory = RoutineListViewModelFactory(repository)


        //ViewModelProvider gives existing RoutineListVM for this fragment, or creates a new one if it does not exist yet
        //this prevents new VM to be created if the fragment is recreated(screen rotation, ...)
        routineListVM = ViewModelProvider(
            this,
            factory
        )[RoutineListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, //where we host the fragment view
        savedInstanceState: Bundle? //contains saved data if android recreated a fragment
    ): View {

        val routineFlow = routineListVM.displayRoutineList()

        //Compose view is not xml but rather a view that can display jetpackCompose
        return ComposeView(requireContext()).apply {//apply: Create this object, then run this block on that object, then return the object.
            setContent {
                //collectAsState is compose function that collects values from flow and converts them into compose state
                val routinesState = routineFlow.collectAsState(initial = emptyList()) //if flow does not emmit immediately use empty list
                val routines = routinesState.value

                RoutineListScreen(
                    routines = routines,

                    onRoutineClick = { item ->

                        lifecycleScope.launch {
                            val suggestion = routineListVM.getRecommendation(item.id).suggestion

                            val bundle = Bundle().apply {
                                putLong("id", item.id)
                                putString("suggestion", suggestion)
                            }

                           findNavController().navigate(
                               R.id.action_routineListFragment_to_routineDetailsFragment,
                               bundle
                           )
                        }
                    },

                    onRoutineDelete = { item ->
                        val routine = Routine(
                            id = item.id,
                            name = item.name,
                            type = item.type,
                            startTime = item.startTime,
                            endTime = item.endTime,
                            daysActive = item.daysActive,
                            notificationEnabled = item.notificationEnabled
                        )

                        val alarmScheduler = AlarmScheduler(requireContext(), routine)
                        alarmScheduler.cancel()

                        viewLifecycleOwner.lifecycleScope.launch {
                            routineListVM.deleteRoutine(routine)
                        }
                    },

                    onAddRoutineClick = {
                        findNavController().navigate(
                            R.id.action_routineListFragment_to_addEditRoutineFragment
                        )
                    },

                    onCheckExecutionClick = {
                        val oneTimeWork =
                            OneTimeWorkRequestBuilder<RoutineEvaluationWorker>()
                                .build()

                        WorkManager
                            .getInstance(requireContext())
                            .enqueue(oneTimeWork)
                    }
                )
            }
        }
    }
}

@Composable
fun RoutineListScreen(
    routines: List<RoutineListDisplay>,
    onRoutineClick: (RoutineListDisplay) -> Unit,
    onRoutineDelete: (RoutineListDisplay) -> Unit,
    onAddRoutineClick: () -> Unit,
    onCheckExecutionClick: () -> Unit
) {
    //used remember here so compose keeps the value of this value, it does not
    //reset every time the ui is redrawn
    var routineToDelete by remember {
        mutableStateOf<RoutineListDisplay?>(null)
    }


    //when we want to stack things on top of each other, used like root container for everything
    Box(
        modifier = Modifier.fillMaxSize() //fills whole screen
    ) {
        //replacement for recView
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            //this is instead of adapter items
            items(
                items = routines,
                key = { routine -> routine.id } //gives each row identity
            ) { routine ->
                //runs for each of the routines (gives a ui for each row with defined behaviour)
                RoutineRow(
                    routine = routine,
                    onClick = {
                        onRoutineClick(routine)
                    },
                    onLongClick = {
                        routineToDelete = routine
                    }
                )
            }
        }

        //fab for adding routine
        FloatingActionButton(
            onClick = onAddRoutineClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = colorResource(id = android.R.color.holo_blue_dark),
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_input_add),
                contentDescription = "Add routine"
            )
        }

        //fab for executing work manager
        FloatingActionButton(
            onClick = onCheckExecutionClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            containerColor = colorResource(id = android.R.color.holo_red_dark),
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = "Check execution"
            )
        }
    }

    if (routineToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                routineToDelete = null
            },
            title = {
                Text("Delete routine?")
            },
            text = {
                Text("Are you sure you want to delete this routine?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        //let takes value on the left side and gives it a temporaty name
                        //inside the block
                        routineToDelete?.let { routine ->
                            onRoutineDelete(routine)
                        }

                        routineToDelete = null
                    }
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        routineToDelete = null
                    }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun RoutineRow(
    routine: RoutineListDisplay,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onClick()
                    },
                    onLongPress = {
                        onLongClick()
                    }
                )
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = routine.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Previously completed:",
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${routine.startTime} - ${routine.endTime}",
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            if(routine.completed == null) {
                Icon(
                    painterResource(id = android.R.drawable.ic_menu_help),
                    contentDescription = "Execution status",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Blue
                )
            }
            else if(routine.completed) {

                Icon(
                    painter = painterResource(id = R.drawable.completed_image),
                    contentDescription = "Execution status",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF228B22) // darker green color
                )
            }else {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_delete),
                    contentDescription = "Execution status",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Red
                )
            }

        }
    }
}