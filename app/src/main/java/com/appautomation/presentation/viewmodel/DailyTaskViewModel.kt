package com.appautomation.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appautomation.data.model.DailyTask
import com.appautomation.data.model.TaskType
import com.appautomation.data.repository.DailyTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DailyTaskViewModel @Inject constructor(
    private val repository: DailyTaskRepository
) : ViewModel() {

    // Selected date in format yyyy-MM-dd (default today)
    private val _selectedDate = MutableStateFlow(getToday())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Grouped tasks by type for the selected date
    private val _tasksGrouped = MutableStateFlow<Map<TaskType, List<DailyTask>>>(emptyMap())
    val tasksGrouped: StateFlow<Map<TaskType, List<DailyTask>>> = _tasksGrouped.asStateFlow()

    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var collectJob: Job? = null

    init {
        loadTasksForDate(_selectedDate.value)
    }

    /** Update selected date and reload tasks */
    fun setDate(date: String) {
        _selectedDate.value = date
        loadTasksForDate(date)
    }

    private fun loadTasksForDate(date: String) {
        // Cancel previous collection job
        collectJob?.cancel()
        
        isLoading = true
        errorMessage = null
        
        collectJob = viewModelScope.launch {
            repository.getTasksGroupedByType(date)
                .onStart { 
                    isLoading = true 
                }
                .catch { e ->
                    errorMessage = e.localizedMessage ?: "Error loading tasks"
                    isLoading = false
                }
                .collect { grouped ->
                    _tasksGrouped.value = grouped
                    isLoading = false
                }
        }
    }

    /** Helper to get today's date string */
    private fun getToday(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}
