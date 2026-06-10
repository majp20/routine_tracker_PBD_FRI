package si.uni_lj.fri.pbd.routinetracker.ui.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import data.entity.Routine
import java.util.Calendar

class AlarmScheduler(private val context: Context, private val routine: Routine) {

    companion object {
        private const val TAG = "AlarmScheduler"
    }

    fun schedule(time : Int? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        val advanceMins = time ?: preferences
            .getString("notification_advance_time", "10")
            ?.toIntOrNull() ?: 10 //if the stored value cannot be converted to int, use 10

        //creates an intent, that sends data to the routineAlarmReceiver
        val intent = Intent(context, RoutineAlarmReceiver::class.java)
        intent.putExtra("routine_id", routine.id)


        //wraps intent, keeps it and sends it later
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routine.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = routine.startTime.split(":")
        if (parts.size != 2) {
            return
        }

        val hours = parts[0].toIntOrNull()
        val minutes = parts[1].toIntOrNull()

        if (hours == null || minutes == null) {
            return
        }


        val activeDayNumbers = routine.daysActive
            .split(",")
            .map { it.trim() }
            .map { dayStringToCalendarDay(it) }


        val currTime = System.currentTimeMillis()

        for (offsetDays in 0..6) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, offsetDays) //ads offset days to current day

            val checkedDay = calendar.get(Calendar.DAY_OF_WEEK)

            if (checkedDay in activeDayNumbers) {

                //sets the scheduled time for activity
                calendar.set(Calendar.HOUR_OF_DAY, hours)
                calendar.set(Calendar.MINUTE, minutes)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)


                //calculates the differences between current time, and advance time for
                //notification -> we get the time at which notification should fire

                calendar.add(Calendar.MINUTE, -advanceMins)

                val triggerMillis = calendar.timeInMillis

                //if the trigger time is already past current time, we are too late
                if (triggerMillis < currTime) {
                    continue
                }


                //only allow this permission if we have android 12 or newer
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val canSchedule = alarmManager.canScheduleExactAlarms()
                    if (!canSchedule) {
                        return
                    }
                }

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, //wake device if needed
                    triggerMillis, //time at which alarm is set
                    pendingIntent //what we send
                )

                return
            }
        }

    }

    fun cancel() {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, RoutineAlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routine.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    private fun dayStringToCalendarDay(day: String): Int {
        return when (day) {
            "Monday" -> Calendar.MONDAY
            "Tuesday" -> Calendar.TUESDAY
            "Wednesday" -> Calendar.WEDNESDAY
            "Thursday" -> Calendar.THURSDAY
            "Friday" -> Calendar.FRIDAY
            "Saturday" -> Calendar.SATURDAY
            "Sunday" -> Calendar.SUNDAY
            else -> throw IllegalArgumentException("Unknown day: $day")
        }
    }
}