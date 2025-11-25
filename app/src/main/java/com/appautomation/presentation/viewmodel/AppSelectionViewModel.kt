package com.appautomation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appautomation.data.model.AppInfo
import com.appautomation.data.model.AppTask
import com.appautomation.service.AppLauncher
import com.appautomation.service.AutomationManager
import com.appautomation.util.Constants
import com.appautomation.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val appLauncher: AppLauncher,
    private val automationManager: AutomationManager
) : ViewModel() {
    
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()
    
    private val _selectedApps = MutableStateFlow<Map<String, AppTask>>(emptyMap())
    val selectedApps: StateFlow<Map<String, AppTask>> = _selectedApps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadInstalledApps()
    }
    
    fun loadInstalledApps(includeSystemApps: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val apps = appLauncher.getInstalledApps(includeSystemApps)
            _installedApps.value = apps
            _isLoading.value = false
        }
    }
    
    fun toggleAppSelection(app: AppInfo, isSelected: Boolean) {
        val currentMap = _selectedApps.value.toMutableMap()
        if (isSelected) {
            currentMap[app.packageName] = AppTask(
                packageName = app.packageName,
                appName = app.appName,
                durationMillis = Constants.DEFAULT_DURATION_MILLIS
            )
        } else {
            currentMap.remove(app.packageName)
        }
        _selectedApps.value = currentMap
    }
    
    fun updateDuration(packageName: String, durationMinutes: Int) {
        val currentMap = _selectedApps.value.toMutableMap()
        val task = currentMap[packageName]
        if (task != null) {
            currentMap[packageName] = task.copy(durationMillis = durationMinutes * 60 * 1000L)
            _selectedApps.value = currentMap
        }
    }
    
    fun selectAll() {
        val allSelected = _installedApps.value.associate { app ->
            app.packageName to AppTask(
                packageName = app.packageName,
                appName = app.appName,
                durationMillis = Constants.DEFAULT_DURATION_MILLIS
            )
        }
        _selectedApps.value = allSelected
    }
    
    fun clearAll() {
        _selectedApps.value = emptyMap()
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun getFilteredApps(): List<AppInfo> {
        val query = _searchQuery.value.lowercase()
        return if (query.isEmpty()) {
            _installedApps.value
        } else {
            _installedApps.value.filter {
                it.appName.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query)
            }
        }
    }
    
    fun startAutomation(): Boolean {
        val tasks = _selectedApps.value.values.toList()
        if (tasks.isEmpty()) {
            return false
        }
        
        automationManager.startAutomation(tasks)
        return true
    }
}
