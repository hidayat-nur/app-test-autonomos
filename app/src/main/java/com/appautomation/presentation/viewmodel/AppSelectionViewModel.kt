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
    
    private val _batchSize = MutableStateFlow(Constants.DEFAULT_BATCH_SIZE)
    val batchSize: StateFlow<Int> = _batchSize.asStateFlow()
    
    private val _currentBatchIndex = MutableStateFlow(0)
    val currentBatchIndex: StateFlow<Int> = _currentBatchIndex.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _testedAppsToday = MutableStateFlow<Set<String>>(emptySet())
    val testedAppsToday: StateFlow<Set<String>> = _testedAppsToday.asStateFlow()
    
    init {
        loadGlobalDuration()
        loadBatchSettings()
        loadTestedAppsToday()
        loadSavedSelections()
        loadInstalledApps()
    }
    
    private fun loadGlobalDuration() {
        _globalDurationMinutes.value = prefs.getInt(
            Constants.PREF_GLOBAL_DURATION,
            Constants.DEFAULT_DURATION_MINUTES
        )
    }
    
    private fun loadBatchSettings() {
        _batchSize.value = prefs.getInt(
            Constants.PREF_BATCH_SIZE,
            Constants.DEFAULT_BATCH_SIZE
        )
        _currentBatchIndex.value = prefs.getInt(
            Constants.PREF_CURRENT_BATCH_INDEX,
            0
        )
    }
    
    private fun loadTestedAppsToday() {
        val today = getTodayDate()
        val lastTestDate = prefs.getString(Constants.PREF_LAST_TEST_DATE, "")
        
        if (today == lastTestDate) {
            // Same day - load tested apps
            val tested = prefs.getStringSet(Constants.PREF_TESTED_APPS_TODAY, emptySet()) ?: emptySet()
            _testedAppsToday.value = tested
        } else {
            // New day - clear tested apps and reset batch index so user starts fresh each day
            _testedAppsToday.value = emptySet()
            _currentBatchIndex.value = 0
            prefs.edit().apply {
                putStringSet(Constants.PREF_TESTED_APPS_TODAY, emptySet())
                putString(Constants.PREF_LAST_TEST_DATE, today)
                putInt(Constants.PREF_CURRENT_BATCH_INDEX, 0)
                apply()
            }
        }
    }
    
    private fun getTodayDate(): String {
        val calendar = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
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
        val globalDuration = if (Constants.TEST_MODE) Constants.TEST_DURATION_MILLIS else _globalDurationMinutes.value * 60 * 1000L
        
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
            val duration = if (Constants.TEST_MODE) Constants.TEST_DURATION_MILLIS else _globalDurationMinutes.value * 60 * 1000L
            currentMap[app.packageName] = AppTask(
                packageName = app.packageName,
                appName = app.appName,
                durationMillis = duration
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
            val newDuration = if (Constants.TEST_MODE) Constants.TEST_DURATION_MILLIS else durationMinutes * 60 * 1000L
            task.copy(durationMillis = newDuration)
        }
        _selectedApps.value = updatedMap
    }
    
    fun updateBatchSize(size: Int) {
        _batchSize.value = size
        prefs.edit().putInt(Constants.PREF_BATCH_SIZE, size).apply()
    }
    
    fun selectAll() {
        val globalDuration = if (Constants.TEST_MODE) Constants.TEST_DURATION_MILLIS else _globalDurationMinutes.value * 60 * 1000L
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
        val allTasks = _selectedApps.value.values.toList()
        if (allTasks.isEmpty()) {
            return false
        }
        
        // Get current batch
        val batchTasks = getCurrentBatch(allTasks)
        if (batchTasks.isEmpty()) {
            // No more batches - reset to beginning
            _currentBatchIndex.value = 0
            prefs.edit().putInt(Constants.PREF_CURRENT_BATCH_INDEX, 0).apply()
            return false
        }
        
        // Mark current batch as tested today
        markAppsAsTested(batchTasks.map { it.packageName }.toSet())
        
        automationManager.startAutomation(batchTasks)
        return true
    }
    
    private fun getCurrentBatch(allTasks: List<AppTask>): List<AppTask> {
        val startIndex = _currentBatchIndex.value * _batchSize.value
        if (startIndex >= allTasks.size) {
            return emptyList()
        }
        val endIndex = minOf(startIndex + _batchSize.value, allTasks.size)
        return allTasks.subList(startIndex, endIndex)
    }
    
    fun moveToNextBatch() {
        _currentBatchIndex.value += 1
        prefs.edit().putInt(Constants.PREF_CURRENT_BATCH_INDEX, _currentBatchIndex.value).apply()
    }
    
    fun setBatchIndex(index: Int) {
        _currentBatchIndex.value = index
        prefs.edit().putInt(Constants.PREF_CURRENT_BATCH_INDEX, index).apply()
    }
    
    fun resetBatchIndex() {
        _currentBatchIndex.value = 0
        prefs.edit().putInt(Constants.PREF_CURRENT_BATCH_INDEX, 0).apply()
    }
    
    fun getTotalBatches(): Int {
        val totalApps = _selectedApps.value.size
        if (totalApps == 0) return 0
        return (totalApps + _batchSize.value - 1) / _batchSize.value
    }
    
    fun hasMoreBatches(): Boolean {
        val totalBatches = getTotalBatches()
        return _currentBatchIndex.value < totalBatches - 1
    }
    
    fun markAppsAsTested(packageNames: Set<String>) {
        val currentTested = _testedAppsToday.value.toMutableSet()
        currentTested.addAll(packageNames)
        _testedAppsToday.value = currentTested
        
        val today = getTodayDate()
        prefs.edit().apply {
            putStringSet(Constants.PREF_TESTED_APPS_TODAY, currentTested)
            putString(Constants.PREF_LAST_TEST_DATE, today)
            apply()
        }
    }
    
    fun isAppTestedToday(packageName: String): Boolean {
        return _testedAppsToday.value.contains(packageName)
    }

    /**
     * Request uninstall for all currently selected apps.
     * Each app will trigger the system uninstall dialog one by one.
     * Returns the list of package names that were requested for uninstall.
     */
    fun getSelectedPackagesForUninstall(): List<String> {
        return _selectedApps.value.keys.toList()
    }

    /**
     * Request uninstall for a single app and remove it from selection.
     */
    fun requestUninstallApp(packageName: String): Boolean {
        val result = appLauncher.requestUninstallApp(packageName)
        if (result) {
            // Remove from selection
            val currentMap = _selectedApps.value.toMutableMap()
            currentMap.remove(packageName)
            _selectedApps.value = currentMap
            saveSelections()
        }
        return result
    }

    /**
     * Request uninstall for the first selected app (for sequential uninstall flow).
     * Returns the package name if successful, null otherwise.
     */
    fun requestUninstallFirstSelected(): String? {
        val firstPackage = _selectedApps.value.keys.firstOrNull() ?: return null
        return if (requestUninstallApp(firstPackage)) firstPackage else null
    }
    
    /**
     * Uninstall all selected apps one by one.
     * Each app will trigger the system uninstall dialog.
     */
    fun uninstallAllSelected() {
        viewModelScope.launch(Dispatchers.Main) {
            val packagesToUninstall = _selectedApps.value.keys.toList()
            for (packageName in packagesToUninstall) {
                requestUninstallApp(packageName)
                // Small delay between uninstalls to allow system dialog to appear
                kotlinx.coroutines.delay(500)
            }
        }
    }
    
    /**
     * Refresh the installed apps list and remove uninstalled apps from selection.
     * This is called after uninstall to update the UI.
     */
    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            // Reload all installed apps
            val apps = appLauncher.getInstalledApps(false)
            _installedApps.value = apps
            
            // Remove uninstalled apps from selection
            val installedPackages = apps.map { it.packageName }.toSet()
            val currentSelection = _selectedApps.value.toMutableMap()
            
            // Remove apps that are no longer installed
            val uninstalledPackages = currentSelection.keys.filter { !installedPackages.contains(it) }
            uninstalledPackages.forEach { pkg ->
                currentSelection.remove(pkg)
            }
            
            if (uninstalledPackages.isNotEmpty()) {
                _selectedApps.value = currentSelection
                saveSelections()
            }
        }
    }
}
