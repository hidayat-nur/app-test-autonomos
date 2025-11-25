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
        private const val PREF_APP_DURATIONS = "app_durations"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()
    
    private val _selectedApps = MutableStateFlow<Map<String, AppTask>>(emptyMap())
    val selectedApps: StateFlow<Map<String, AppTask>> = _selectedApps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadSavedSelections()
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
    
    private fun loadSavedSelections() {
        val savedApps = prefs.getStringSet(PREF_SELECTED_APPS, emptySet()) ?: emptySet()
        val savedDurations = prefs.getString(PREF_APP_DURATIONS, "{}") ?: "{}"
        
        val durationsMap = parseDurationsJson(savedDurations)
        
        val restoredSelections = savedApps.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 2) {
                val pkg = parts[0]
                val appName = parts[1]
                val duration = durationsMap[pkg] ?: Constants.DEFAULT_DURATION_MILLIS
                
                pkg to AppTask(
                    packageName = pkg,
                    appName = appName,
                    durationMillis = duration
                )
            } else null
        }.toMap()
        
        _selectedApps.value = restoredSelections
    }
    
    private fun saveSelections() {
        val selectedSet = _selectedApps.value.map { (pkg, task) ->
            "$pkg|${task.appName}"
        }.toSet()
        
        val durationsJson = buildDurationsJson(_selectedApps.value)
        
        prefs.edit().apply {
            putStringSet(PREF_SELECTED_APPS, selectedSet)
            putString(PREF_APP_DURATIONS, durationsJson)
            apply()
        }
    }
    
    private fun buildDurationsJson(tasks: Map<String, AppTask>): String {
        return tasks.entries.joinToString(",", "{", "}") { (pkg, task) ->
            "\"$pkg\":${task.durationMillis}"
        }
    }
    
    private fun parseDurationsJson(json: String): Map<String, Long> {
        if (json == "{}") return emptyMap()
        
        return try {
            json.removeSurrounding("{", "}")
                .split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val pkg = parts[0].trim().removeSurrounding("\"")
                        val duration = parts[1].trim().toLongOrNull()
                        if (duration != null) pkg to duration else null
                    } else null
                }
                .toMap()
        } catch (e: Exception) {
            emptyMap()
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
        saveSelections()
    }
    
    fun updateDuration(packageName: String, durationMinutes: Int) {
        val currentMap = _selectedApps.value.toMutableMap()
        val task = currentMap[packageName]
        if (task != null) {
            currentMap[packageName] = task.copy(durationMillis = durationMinutes * 60 * 1000L)
            _selectedApps.value = currentMap
            saveSelections()
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
