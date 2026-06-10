package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import repository.RoutineRepository

class RoutineDetailViewModelFactory(
    private val repository: RoutineRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}