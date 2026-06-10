package viewmodel

import androidx.lifecycle.ViewModel
import data.entity.Routine
import data.entity.RoutineExecution
import kotlinx.coroutines.flow.Flow
import repository.RoutineRepository

class RoutineDetailViewModel(private val repository: RoutineRepository) : ViewModel() {

    suspend fun deleteRoutine(routine : Routine) {
        repository.deleteRoutine(routine)
    }
    fun getRoutineById(id: Long): Flow<Routine?> {
        return repository.getRoutineById(id)
    }

    suspend fun getRoutineExecutionHistory(routineId: Long) : List<RoutineExecution> {
        return repository.getRoutineStatusHistory(routineId)
    }


}