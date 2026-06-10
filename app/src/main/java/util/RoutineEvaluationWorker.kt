package util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import data.RoutinesDatabase
import data.entity.RoutineExecution
import kotlinx.coroutines.flow.first
import repository.RoutineRepository
import java.util.Calendar

class RoutineEvaluationWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private  val TAG = "RoutineWorker"
    }


    override suspend fun doWork(): Result {


        val db = RoutinesDatabase.getDatabase(applicationContext)
        val dao = db.routineDao()
        val repo = RoutineRepository(dao, applicationContext )

        val sharedPreferences =
            applicationContext.getSharedPreferences("UserPreferences", MODE_PRIVATE)

        val lastForegroundTime = sharedPreferences.getLong("lastForegroundTime", 0L)
        val lastBackgroundTime = sharedPreferences.getLong("lastBackgroundTime", 0L)


        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
        val currTime = System.currentTimeMillis()


        val routines = repo.getAllRoutines().first()


        for (routine in routines) {
            Log.d(
                TAG,
                "Checking routine -> id=${routine.id}, name=${routine.name}, daysActive=${routine.daysActive}, start=${routine.startTime}, end=${routine.endTime}"
            )

            val activeDayNumbers = routine.daysActive
                .split(",")
                .map { it.trim() }
                .map { dayStringToCalendarDay(it) }

            var isActive = false
            for (activeDay in activeDayNumbers) {
                if (activeDay == day) {
                    isActive = true
                    break
                }
            }

            val startTimeMillis = formatTime(routine.startTime)
            val endTimeMillis = formatTime(routine.endTime)


            if (!isActive) {
                Log.d(TAG, "Skipping routine ${routine.id}: not active today")
                continue
            }

            if (currTime < startTimeMillis) {
                Log.d(TAG, "Skipping routine ${routine.id}: before start time")
                continue
            }

            //if (currTime < endTimeMillis) {
            //    Log.d(TAG, "Skipping routine ${routine.id}: routine still in progress")
             //   continue
            //}

            val completed = checkOverlapping(
                lastForegroundTime,
                lastBackgroundTime,
                startTimeMillis,
                endTimeMillis,
                currTime
            )

            Log.d(
                TAG,
                "Overlap result -> routineId=${routine.id}, completed=$completed"
            )

            val lastExecution = repo.existsForToday(routine.id, today)

            if (lastExecution == 0) {
                val r = RoutineExecution(
                    0L,
                    routine.id,
                    completed,
                    today
                )

                val insertedId = repo.insertRoutineExecution(r)

                Log.d(
                    TAG, "Insert finished -> newExecutionId=$insertedId for routineId=${routine.id}"
                )
            } else {
                Log.d(
                    TAG,
                    "Skipping insert for routineId=${routine.id}: execution already exists for today"
                )
            }
        }
        return Result.success()
    }
    private fun formatTime(routineTime : String) : Long {
        val parts = routineTime.split(":")

        val h = parts[0].toInt()
        val m = parts[1].toInt()

       val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, h)
        calendar.set(Calendar.MINUTE, m)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }
    private fun checkOverlapping(
        lastForegroundTime: Long,
        lastBackgroundTime: Long,
        rStartTime: Long,
        rEndTime: Long,
        currTime: Long
    ): Boolean {

        val foregroundInsideRoutine =
            lastForegroundTime >= rStartTime && lastForegroundTime < rEndTime

        val backgroundInsideRoutine =
            lastBackgroundTime > rStartTime && lastBackgroundTime <= rEndTime

        val appCurrentlyOpenDuringRoutine =
            lastForegroundTime > lastBackgroundTime &&
                    lastForegroundTime < rEndTime &&
                    currTime > rStartTime

        return foregroundInsideRoutine ||
                backgroundInsideRoutine ||
                appCurrentlyOpenDuringRoutine
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
