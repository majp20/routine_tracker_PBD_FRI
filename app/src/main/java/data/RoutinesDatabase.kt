package data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import data.dao.RoutineDao
import data.entity.Routine
import data.entity.RoutineExecution


//reference for the code used from the internet, makes sure only 1 instance of the
//db is in the whole app
@Database([Routine::class, RoutineExecution::class], version = 3, exportSchema = false)
abstract class RoutinesDatabase : RoomDatabase() {
    abstract fun routineDao() : RoutineDao
    companion object {
        @Volatile
        private var INSTANCE: RoutinesDatabase? = null

        fun getDatabase(context: Context): RoutinesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoutinesDatabase::class.java,
                    "routine_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}