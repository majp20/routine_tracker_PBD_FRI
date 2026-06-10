package si.uni_lj.fri.pbd.routinetracker.ui.main

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import data.RoutinesDatabase
import data.entity.Routine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import repository.RoutineRepository
import si.uni_lj.fri.pbd.routinetracker.R
import viewmodel.RoutineListViewModel
import viewmodel.RoutineListViewModelFactory

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var routineListVM: RoutineListViewModel

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val dao = RoutinesDatabase.getDatabase(requireContext()).routineDao()
        val repository = RoutineRepository(dao, requireContext().applicationContext)
        val factory = RoutineListViewModelFactory(repository)
        routineListVM = ViewModelProvider(this, factory)[RoutineListViewModel::class.java]

        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        //this is a global key that will allow notifications
        val notificationsPreference = findPreference<SwitchPreferenceCompat>("global_notifications_enabled")

        //global key for notification time
        val timePreference = findPreference<EditTextPreference>("notification_advance_time")

        //for city
        val cityPreference = findPreference<EditTextPreference>("current_location")

        //key for reset
        val resetPreference = findPreference<Preference>("reset_app")

        //we need to read current switch value when the fragment opens
        timePreference?.isEnabled = notificationsPreference?.isChecked == true


        //reads the current value of the switch and listens to change
        notificationsPreference?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean //new value of the switch when user changes it
            timePreference?.isEnabled = isEnabled //disables the ability to change notification advance time if notifications disabled

            lifecycleScope.launch {
                val routines = routineListVM.getAllRoutines().first()
                for (routine in routines) {
                    val scheduler = AlarmScheduler(requireContext(), routine)
                    if (isEnabled) {
                        if (routine.notificationEnabled) {
                            scheduler.schedule()
                        }
                    } else {
                        scheduler.cancel()
                    }
                }
            }
            true
        }

        //only enables numericalInput
        timePreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        //setOnBindEditTextListener enables editing of editText field
        cityPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT
        }

        //reads the current vale of the edit text and listens to change
        timePreference?.setOnPreferenceChangeListener{ _, newValue ->
            val newTime = newValue as String

            val mins = newTime.toInt()
            if(mins <= 0) {
                Toast.makeText(requireContext(),
                    "Enter valid time",
                    Toast.LENGTH_SHORT).show()
                return@setOnPreferenceChangeListener false //false so it does not save the value
            }
            if(notificationsPreference?.isChecked == true) {
                lifecycleScope.launch {
                    val routines = routineListVM.getAllRoutines().first()
                    for (routine in routines) {
                        val scheduler = AlarmScheduler(requireContext(), routine)
                        scheduler.cancel()
                        if (routine.notificationEnabled) {
                            scheduler.schedule(mins)
                        }
                    }
                }
            }

            true //true so it saves the value
        }

        //custom summary provider that displays number + "minutes"
        timePreference?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            val value = preference.text
            val min = value?.toInt() //gets loose of improper times like 003, 0023,...
            "$min minutes"
        }

        //resetting changes
        resetPreference?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset application?")
                .setMessage("This action will delete all routines from the app and reset your settings.")
                .setPositiveButton("RESET") { dialog, which ->
                    lifecycleScope.launch {
                        //delete all routines
                        val allRoutines: List<Routine> = routineListVM.getAllRoutines().first()
                        for (routine in allRoutines) {
                            val scheduler = AlarmScheduler(requireContext(), routine)
                            scheduler.cancel()
                            routineListVM.deleteRoutine(routine)
                        }

                        //change settings to default (notifications ON and time = 10min)
                        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        prefs.edit() //start modifying prefs
                            .clear() //removes all keys
                            .apply() //saves changes

                        //refresh UI for the changes to be seen
                        requireActivity().recreate()
                    }
                }
                .setNegativeButton("CANCEL") { dialog, which ->
                    dialog.dismiss()
                }
                .show()
            true
        }
    }
}