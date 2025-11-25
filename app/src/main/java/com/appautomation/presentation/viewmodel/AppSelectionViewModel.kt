package com.appautomation.presentation.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appautomation.data.model.AppInfo
import com.appautomation.data.model.AppTask
import com.appautomation.service.AppLauncher
import com.appautomation.service.AutomationManager
import com.appautomation.util.Constants
import com.appautomation.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLauncher: AppLauncher,
    private val automationManager: AutomationManager
) : ViewModel() {
    
    companion object {
        private const val PREF_SELECTED_APPS = "selected_apps"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()
    
    private val _selectedApps = MutableStateFlow<Map<String, AppTask>>(emptyMap())
    val selectedApps: StateFlow<Map<String, AppTask>> = _selectedApps.asStateFlow()
    
    private val _globalDurationMinutes = MutableStateFlow(Constants.DEFAULT_DURATION_MINUTES)
    val globalDurationMinutes: StateFlow<Int> = _globalDurationMinutes.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadGlobalDuration()
        loadSavedSelections()
        loadInstalledApps()
    }
    
    private fun loadGlobalDuration() {
        _globalDurationMinutes.value = prefs.getInt(
            Constants.PREF_GLOBAL_DURATION,
            Constants.DEFAULT_DURATION_MINUTES
        )
    }
    
    fun loadInstalledApps(includeSystemApps: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val apps = appLauncher.getInstalledApps(includeSystemApps)
            _installedApps.value = apps
            _isLoading.value = false
        }
    }
    
    private fun loadSavedSelections() {
        val savedApps = prefs.getStringSet(PREF_SELECTED_APPS, emptySet()) ?: emptySet()
        val globalDuration = _globalDurationMinutes.value * 60 * 1000L
        
        val restoredSelections = savedApps.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 2) {
                val pkg = parts[0]
                val appName = parts[1]
                
                pkg to AppTask(
                    packageName = pkg,
                    appName = appName,
                    durationMillis = globalDuration
                )
            } else null
        }.toMap()
        
        _selectedApps.value = restoredSelections
    }
    
    private fun saveSelections() {
        val selectedSet = _selectedApps.value.map { (pkg, task) ->
            "$pkg|${task.appName}"
        }.toSet()
        
        prefs.edit().apply {
            putStringSet(PREF_SELECTED_APPS, selectedSet)
            apply()
        }
    }
    
    fun toggleAppSelection(app: AppInfo, isSelected: Boolean) {
        val currentMap = _selectedApps.value.toMutableMap()
        if (isSelected) {
            currentMap[app.packageName] = AppTask(
                packageName = app.packageName,
                appName = app.appName,
                durationMillis = _globalDurationMinutes.value * 60 * 1000L
            )
        } else {
            currentMap.remove(app.packageName)
        }
        _selectedApps.value = currentMap
        saveSelections()
    }
    
    fun updateGlobalDuration(durationMinutes: Int) {
        _globalDurationMinutes.value = durationMinutes
        prefs.edit().putInt(Constants.PREF_GLOBAL_DURATION, durationMinutes).apply()
        
        // Update all selected apps with new global duration
        val updatedMap = _selectedApps.value.mapValues { (_, task) ->
            task.copy(durationMillis = durationMinutes * 60 * 1000L)
        }
        _selectedApps.value = updatedMap
    }
    
    fun selectAll() {
        val globalDuration = _globalDurationMinutes.value * 60 * 1000L
        val allSelected = _installedApps.value.associate { app ->
            app.packageName to AppTask(
                packageName = app.packageName,
                appName = app.appName,
                durationMillis = globalDuration
            )
        }
        _selectedApps.value = allSelected
        saveSelections()
    }
    
    fun clearAll() {
        _selectedApps.value = emptyMap()
        saveSelections()
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
