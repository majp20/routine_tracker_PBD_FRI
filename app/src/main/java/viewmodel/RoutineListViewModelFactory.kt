package viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import repository.RoutineRepository

class RoutineListViewModelFactory(
    private val repository: RoutineRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutineListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}