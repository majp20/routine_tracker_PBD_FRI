package si.uni_lj.fri.pbd.routinetracker.ui.main

import adapter.RoutineDetailAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import data.RoutinesDatabase
import data.entity.Routine
import data.entity.RoutineExecution
import kotlinx.coroutines.launch
import repository.RoutineRepository
import si.uni_lj.fri.pbd.routinetracker.R
import si.uni_lj.fri.pbd.routinetracker.databinding.FragmentRoutineDetailsBinding
import viewmodel.RoutineDetailViewModel
import viewmodel.RoutineDetailViewModelFactory

class RoutineDetailsFragment : Fragment() {

    private lateinit var binding: FragmentRoutineDetailsBinding
    private lateinit var routineDetailVM: RoutineDetailViewModel
    private lateinit var historyAdapter: RoutineDetailAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentRoutineDetailsBinding.inflate(inflater, container, false)


        val routineId = requireArguments().getLong("id")

        val dao = RoutinesDatabase.getDatabase(requireContext()).routineDao()
        val repository = RoutineRepository(dao, requireContext().applicationContext)
        val factory = RoutineDetailViewModelFactory(repository)
        routineDetailVM = ViewModelProvider(this, factory)[RoutineDetailViewModel::class.java]

        historyAdapter = RoutineDetailAdapter(mutableListOf())
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = historyAdapter

        loadRoutine(routineId)
        loadHistory(routineId)

        return binding.root
    }

    private fun loadRoutine(routineId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            routineDetailVM.getRoutineById(routineId).collect { routine ->
                routine ?: return@collect
                bindRoutine(routine, routineId)
            }
        }
    }
    //function that handles routine ui visualisation and has button click listeners
    //reused logic from part 1
    private fun bindRoutine(routine: Routine, routineId: Long) {
        val formattedDays = routine.daysActive.replace(",", ", ")

        binding.textName.text = "Name: ${routine.name}"
        binding.textType.text = "Type: ${routine.type}"
        binding.textTime.text = "Time range: ${routine.startTime} - ${routine.endTime}"
        binding.daysActive.text = "Active days: $formattedDays"
        binding.notificationsEnabled.text =
            if (routine.notificationEnabled) {
                "Notifications: Enabled"
            } else {
                "Notifications: Disabled"
            }

        val suggestion = requireArguments().getString("suggestion")
        binding.suggestionField.text = suggestion

        binding.editButton.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("id", routineId)
                putString("name", routine.name)
                putString("type", routine.type)
                putString("startTime", routine.startTime)
                putString("endTime", routine.endTime)
                putString("daysActive", routine.daysActive)
                putBoolean("notificationsEnabled", routine.notificationEnabled)
            }

            findNavController().navigate(
                R.id.action_routineDetailsFragment_to_addEditRoutineFragment,
                bundle
            )
        }

        binding.delButton.setOnClickListener {
            val alarmScheduler = AlarmScheduler(requireContext(), routine)
            alarmScheduler.cancel()

            viewLifecycleOwner.lifecycleScope.launch {
                routineDetailVM.deleteRoutine(routine)
                findNavController().navigate(
                    R.id.action_routineDetailsFragment_to_routineListFragment
                )
            }
        }
    }

    //function used to load execution history of a routine via routineDetailViewModel
    private fun loadHistory(routineId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val history = routineDetailVM.getRoutineExecutionHistory(routineId)
            historyAdapter.submitList(history)
        }
    }
}