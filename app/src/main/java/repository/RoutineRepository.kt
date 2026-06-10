package repository

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import data.RoutineContext
import data.dao.RoutineDao
import data.dao.RoutineListDisplay
import data.entity.Routine
import data.entity.RoutineExecution
import data.sensor.LightSensorReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import rest.RestAPI
import rest.RetrofitInstance
import rest.WeatherData

class RoutineRepository(private val routineDao : RoutineDao, private val context: Context)  {
    companion object {
        private const val TAG = "RoutineRepository"
        private val api = RetrofitInstance.createService(RestAPI::class.java)
        private val API_KEY = "0bd8aa0626252aec634f3955a8864ccb"


    }

    suspend fun insertRoutine(routine: Routine) : Long {
        return routineDao.insertRoutine(routine)
    }

    suspend fun insertRoutineExecution(routineE : RoutineExecution) : Long {
        return routineDao.insertRoutineExecution(routineE)
    }

    suspend fun deleteRoutine(routine: Routine) {
        routineDao.deleteRoutine(routine)
    }
    fun getAllRoutines() : Flow<List<Routine>> {
        return routineDao.getAllRoutines()
    }
    fun getRoutineById(id : Long) : Flow<Routine?> {
        return routineDao.getRoutineById(id)
    }
    suspend fun updateRoutine(routine: Routine) {
        routineDao.updateRoutine(routine)
    }
    suspend fun getRoutineStatusHistory(id: Long): List<RoutineExecution> {
        return routineDao.getRoutineStatusHistory(id)
    }

    fun getRoutinesWithStatus(): Flow<List<RoutineListDisplay>> {
        return routineDao.getRoutinesWithStatus()
    }

    suspend fun existsForToday(id : Long, date : String) : Int {
        return routineDao.existsForToday(id, date)
    }


    suspend fun getWeather(location: String): WeatherData {
        try {
            val weatherJSON = api.getWeather(location, API_KEY)

            Log.d("RoutineRepository", "Weather response: $weatherJSON")

            val mainObject = weatherJSON.getAsJsonObject("main")
            val temperature = mainObject.get("temp").asDouble

            val weatherArray = weatherJSON.getAsJsonArray("weather")
            val conditions = weatherArray[0]
                .asJsonObject
                .get("main")
                .asString

            return WeatherData(temperature, conditions)

        } catch (e: Exception) {
            Log.e("RoutineRepository", "Failed to get weather", e)
        }
        return  WeatherData(temperature = 0.0, conditions = "Unknown")
    }



    //function for building routine recommendations

    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    suspend fun buildRoutineContext(routineId : Long): RoutineContext {
        val routine = routineDao.getRoutineById(routineId).first()
        val routineType = routine?.type

        var location = sharedPrefs.getString("current_location", "Ljubljana, Slovenia")
        if(location == null) location = "Ljubljana, Slovenia"

        val weather = getWeather(location)


        val temp = weather.temperature
        val conditions = weather.conditions

        val light = LightSensorReader(context).readOnce()

        val suggestion = getRoutineSuggestion(temp, conditions, routineType, light)
        Log.d(TAG, "location: $location \n temp : $temp\n conditions: $conditions\n light: $light\n type : $routineType")
        Log.d(TAG, suggestion)

        return RoutineContext(temp, conditions, light, suggestion)

    }

    private fun getRoutineSuggestion(temperature : Double, conditions : String, routineType: String?, light : Float) : String {

        when(routineType){
            "Study" -> {
                if(light !in 50.00..500.00) { //range check instead of checking each value individually
                    return "Consider adjusting the light for better focus"
                }
                return "Get ready for some productive studying"
            }
            "Exercise" -> {
                if(temperature !in 5.00..30.00) {
                    return "Consider indoor exercise"
                }
                return "Consider outdoor exercise"

            }
            "Socialise" -> {
                if(conditions.equals("Raining") || conditions.equals("Snowing")) {
                    return "Consider attending an indoor event, for instance, going to theatre"
                }
                return "Consider organising a picnic"

            }
            else -> return "There was a problem with a suggestion"
        }
    }
}