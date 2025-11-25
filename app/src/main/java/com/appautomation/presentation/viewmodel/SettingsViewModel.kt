package com.appautomation.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appautomation.data.repository.AppRepository
import com.appautomation.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) : ViewModel() {
    
    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()
    
    data class PermissionsState(
        val accessibilityEnabled: Boolean = false,
        val usageStatsGranted: Boolean = false,
        val batteryOptimizationDisabled: Boolean = false
    )
    
    init {
        checkPermissions()
    }
    
    fun checkPermissions() {
        _permissionsState.value = PermissionsState(
            accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context),
            usageStatsGranted = PermissionHelper.hasUsageStatsPermission(context),
            batteryOptimizationDisabled = PermissionHelper.isBatteryOptimizationDisabled(context)
        )
    }
    
    fun openAccessibilitySettings() {
        PermissionHelper.openAccessibilitySettings(context)
    }
    
    fun openUsageStatsSettings() {
        PermissionHelper.openUsageStatsSettings(context)
    }
    
    fun requestBatteryOptimizationExemption() {
        PermissionHelper.requestBatteryOptimizationExemption(context)
    }
    
    fun cleanOldLogs() {
        viewModelScope.launch {
            repository.cleanOldLogs(30)
        }
    }
}
