package si.uni_lj.fri.pbd.routinetracker.ui.main

import si.uni_lj.fri.pbd.routinetracker.R
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import data.RoutinesDatabase
import data.dao.RoutineDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import repository.RoutineRepository
import viewmodel.RoutineListViewModel
import viewmodel.RoutineListViewModelFactory


class RoutineAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "routine_channel"
        private val TAG = "RoutineAlarmReceiver"
    }

    //receive a routine
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("routine_id", -1L) //if -1L return immediately

        if (id == -1L) {
            return
        }

        //tells android not to kill this receiver, because it has async work to do
        val pendingResult = goAsync()

        //starts a coroutine on the IO thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = RoutinesDatabase.getDatabase(context).routineDao()
                val repository = RoutineRepository(dao, context.applicationContext)
                val routine = repository.getRoutineById(id).first() //receives 1st value from flow

                if (routine == null) {
                    return@launch
                }

                //preferences in settings
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val enabled = prefs.getBoolean("global_notifications_enabled", true)
                if (!enabled) {
                    return@launch
                }

                //intent for opening app (when user taps on notification)
                val openIntent = Intent(context, MainActivity::class.java)
                openIntent.putExtra("routine_id", id)
                openIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

                //this is for clicking on notification that will open specific routine
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    id.toInt(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val recommendation = repository.buildRoutineContext(id).suggestion

                //from ucilnica on how we build notification
                val name = routine.name
                val notificationTitle = "Upcoming routine $name"

                //notifications must belong to a specific channel
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(notificationTitle)
                    .setContentText(recommendation)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentIntent(pendingIntent)//what happens when user taps notification
                    .setAutoCancel(true) //notification disappears after it is clicked
                    .build()

                //show notification if notifications for app are allowed
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    //switches to main dispatcher to and shows notification
                    withContext(Dispatchers.Main) {
                        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
                    }

                    //this will schedule next notification for the specific routine(next active day)
                    if (routine.notificationEnabled) {
                        val alarmScheduler = AlarmScheduler(context, routine)
                        alarmScheduler.schedule()
                    }
                }
            } finally { //finally means no matter what happens execute this
                pendingResult.finish()
            }
        }
    }
}