package data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import data.entity.Routine
import data.entity.RoutineExecution
import kotlinx.coroutines.flow.Flow

data class RoutineListDisplay(
    val id: Long,
    val name: String,
    val type: String,
    val startTime: String,
    val endTime: String,
    val completed: Boolean?,
    val daysActive: String,
    val notificationEnabled: Boolean,
)

@Dao
interface RoutineDao {

    @Insert
    suspend fun insertRoutine(routine: Routine): Long

    @Insert
    suspend fun insertRoutineExecution(routineE : RoutineExecution) : Long

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("SELECT * FROM Routine")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM Routine WHERE id = :id")
    fun getRoutineById(id: Long): Flow<Routine?>

    @Update
    suspend fun updateRoutine(routine: Routine)

    // function for routineListViewModel returns a list of routines with only display info and
    //their execution status
    @Query("""
    SELECT 
        r.id,
        r.name,
        r.type,
        r.startTime,
        r.endTime,
        re.completed AS completed,
        r.daysActive,
        r.notificationEnabled
    FROM Routine r
    LEFT JOIN RoutineExecution re
        ON re.id = (
            SELECT MAX(re2.id)
            FROM RoutineExecution re2
            WHERE re2.routineId = r.id
        )
    ORDER BY r.id ASC """)
    fun getRoutinesWithStatus(): Flow<List<RoutineListDisplay>>

    // function to see past completed statuses of specific routine
    @Query("SELECT * FROM RoutineExecution WHERE routineId = :id ORDER BY id DESC")
    suspend fun getRoutineStatusHistory(id: Long): List<RoutineExecution>


    //function only used in work manager used to find out if routineExecution instance already
    //exists for a current routine
    @Query("SELECT COUNT(*) FROM RoutineExecution WHERE routineId = :routineId AND date = :date")
    suspend fun existsForToday(routineId: Long, date: String): Int
}