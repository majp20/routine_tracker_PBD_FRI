package si.uni_lj.fri.pbd.routinetracker.ui.main

import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import data.RoutinesDatabase
import data.entity.Routine
import kotlinx.coroutines.launch
import repository.RoutineRepository
import si.uni_lj.fri.pbd.routinetracker.R
import si.uni_lj.fri.pbd.routinetracker.databinding.FragmentAddEditRoutineBinding
import viewmodel.RoutineListViewModel
import viewmodel.RoutineListViewModelFactory


class AddEditRoutineFragment : Fragment() {

    private lateinit var binding: FragmentAddEditRoutineBinding

    private lateinit var typeAdapter: ArrayAdapter<CharSequence>

    private lateinit var routineListVM: RoutineListViewModel

    companion object {
        private val TAG = "AddEditRoutineFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentAddEditRoutineBinding.inflate(inflater, container, false)

        val dao = RoutinesDatabase.getDatabase(requireContext()).routineDao()
        val repository = RoutineRepository(dao, requireContext().applicationContext) //pass the whole application context
        val factory = RoutineListViewModelFactory(repository)
        routineListVM = ViewModelProvider(this, factory)[RoutineListViewModel::class.java]

        val id = arguments?.getLong("id")

        //array adapter is used to connect a list of text to UI component (spinner)
        //controls how the spinner looks when closed
        typeAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.routine_types,
            android.R.layout.simple_spinner_item
        )

        //this line controls how each item looks when the spinner is opened
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.typeSpinner.adapter = typeAdapter


        //time listener
        binding.editStartTime.setOnClickListener {
            showTimePicker(binding.editStartTime)
        }

        binding.editEndTime.setOnClickListener {
            showTimePicker(binding.editEndTime)
        }

        if(id != null) {
            //we are in edit mode, we need to set routine in edit mode
            setRoutine()
        }

        val saveButton = binding.saveButton
        saveButton.setOnClickListener {
            if(id != null) saveRoutine(id)
            else saveRoutine(null)
        }

        return binding.root
    }


    //function used to set already saved routine
    private fun setRoutine() {


        val name = arguments?.getString("name")
        val type = arguments?.getString("type")
        val startTime = arguments?.getString("startTime")
        val endTime = arguments?.getString("endTime")
        val daysActive = arguments?.getString("daysActive")
        val notificationsEnabled = arguments?.getBoolean("notificationsEnabled")

        //set name
        binding.editName.setText(name)

        //set type
        val position = typeAdapter.getPosition(type) //gets position from type adapter
        binding.typeSpinner.setSelection(position)

        //set time
        binding.editStartTime.setText(startTime)
        binding.editEndTime.setText(endTime)

        //set active days
        //days are saved in db formated like: Monday,Tuesday,Thursday, so we need to preprocess them
        val selectedDays = daysActive?.split(",")?.map { it.trim() }
        //elvis operator: if day is not in active days, use empty list -> resulting in false (the box is unchecked)
        binding.monday.isChecked = "Monday" in (selectedDays ?: emptyList())
        binding.tuesday.isChecked = "Tuesday" in (selectedDays ?: emptyList())
        binding.wednesday.isChecked = "Wednesday" in (selectedDays ?: emptyList())
        binding.thursday.isChecked = "Thursday" in (selectedDays ?: emptyList())
        binding.friday.isChecked = "Friday" in (selectedDays ?: emptyList())
        binding.saturday.isChecked = "Saturday" in (selectedDays ?: emptyList())
        binding.sunday.isChecked = "Sunday" in (selectedDays ?: emptyList())

        //switch
        if(notificationsEnabled != null)binding.notficationsSwitch.isChecked = notificationsEnabled

    }

    //function that saves new or edited routine
    private fun saveRoutine(id : Long?) {
        val name = binding.editName.text.toString().trim().ifEmpty { "Unnamed routine" } //sets a default name, trim trims spaces,so they don't count as name
        val type = binding.typeSpinner.selectedItem.toString()

        //days
        val selectedDays = mutableListOf<String>()

        if (binding.monday.isChecked) selectedDays.add("Monday")
        if (binding.tuesday.isChecked) selectedDays.add("Tuesday")
        if (binding.wednesday.isChecked) selectedDays.add("Wednesday")
        if (binding.thursday.isChecked) selectedDays.add("Thursday")
        if (binding.friday.isChecked) selectedDays.add("Friday")
        if (binding.saturday.isChecked) selectedDays.add("Saturday")
        if (binding.sunday.isChecked) selectedDays.add("Sunday")

        val days = selectedDays.joinToString(",")

        val notifications = binding.notficationsSwitch.isChecked

        val startTime = binding.editStartTime.text.toString().trim().ifEmpty { "00:00" } //if empty fill this default value
        val endTime = binding.editEndTime.text.toString().trim().ifEmpty { "23:59" }

        //at least one active day must be selected
        if(selectedDays.isEmpty()) {
            //error here, days must be fulfilled
            Toast.makeText(requireContext(),
                "You must select at least one day to be active!",
                Toast.LENGTH_SHORT).show()
            return //returns so we cant save
        }
        //routine start time < routine end time
        if(compareTime(startTime, endTime)){
            Toast.makeText(requireContext(), //toast message for violating requirements
                "Activity start time must be earlier than activity end time!",
                Toast.LENGTH_SHORT).show()
            return
        }

        //If a routine is new we create it
        if(id == null) {
            val routine = Routine(0L, name, type, startTime, endTime, days, notifications)

            Log.d(TAG, "Creating routine with input values ->" +
                    " name=$name, type=$type, startTime=$startTime, endTime=$endTime, days=$days, notifications=$notifications")

            lifecycleScope.launch {
                try {
                    val insertedId = routineListVM.insertRoutine(routine)

                    //if inserting fails return value is -1L
                    if (insertedId == -1L) {
                        Log.d(TAG, "Routine failed to insert inside fragment")
                        return@launch //returns only from launch
                    }

                    val savedRoutine = Routine(insertedId, name, type, startTime, endTime, days, notifications)
                    val alarmScheduler = AlarmScheduler(requireContext(), savedRoutine)


                    //reeds the preferences for global settings enabled
                    val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    val globalEnabled = prefs.getBoolean("global_notifications_enabled", true) //default = true

                    if (routine.notificationEnabled && globalEnabled) {
                        alarmScheduler.schedule()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Insert failed: ${e.message}", e)
                }
            }

        }else {
            //just update
            val routine = Routine(id, name, type, startTime, endTime, days, notifications)
            lifecycleScope.launch {
                routineListVM.updateRoutine(routine)
                val alarmScheduler = AlarmScheduler(requireContext(), routine)
                //can send notifications oly when they are enabled for routine and globally
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val globalEnabled = prefs.getBoolean("global_notifications_enabled", true)
                if (routine.notificationEnabled && globalEnabled) {
                    alarmScheduler.schedule()
                }
            }
        }
        //switching back to fragmentList
        findNavController().navigate(R.id.action_addEditRoutineFragment_to_routineListFragment)
    }

    //function for selected user time input and for formating the selected time
    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                editText.setText(formattedTime)
            },
            hour,
            minute,
            true
        )

        timePicker.show()
    }
    private fun compareTime(stTime : String, endTime : String) : Boolean {
        val minSt = convertToMin(stTime)
        val minEnd = convertToMin(endTime)
        return minSt >= minEnd

    }
    private fun convertToMin(time : String) : Int {
        val hM = time.split(":").map {it.toInt()}
        return hM[0]*60+hM[1]
    }
}