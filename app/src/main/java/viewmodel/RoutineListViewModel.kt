package viewmodel

import androidx.lifecycle.ViewModel
import data.RoutineContext
import data.dao.RoutineListDisplay
import data.entity.Routine
import kotlinx.coroutines.flow.Flow
import repository.RoutineRepository

class RoutineListViewModel(private val repository: RoutineRepository) : ViewModel() {

    fun displayRoutineList(): Flow<List<RoutineListDisplay>> {
        return repository.getRoutinesWithStatus()
    }
    suspend fun deleteRoutine(routine : Routine) {
        repository.deleteRoutine(routine)
    }

    suspend fun updateRoutine(routine: Routine) {
        repository.updateRoutine(routine)
    }
    suspend fun insertRoutine(routine: Routine) : Long {
        return repository.insertRoutine(routine)
    }

    fun getAllRoutines() : Flow<List<Routine>>  {
        return repository.getAllRoutines()
    }

    suspend fun getRecommendation(id : Long) : RoutineContext {
        return repository.buildRoutineContext(id)
    }

}