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
        val batteryOptimizationDisabled: Boolean = false,
        val notificationPermissionGranted: Boolean = false,
        val overlayPermissionGranted: Boolean = false
    )
    
    init {
        checkPermissions()
    }
    
    fun checkPermissions() {
        val overlayGranted = android.provider.Settings.canDrawOverlays(context)
        android.util.Log.d("SettingsViewModel", "🔍 Checking permissions...")
        android.util.Log.d("SettingsViewModel", "  Accessibility: ${PermissionHelper.isAccessibilityServiceEnabled(context)}")
        android.util.Log.d("SettingsViewModel", "  Usage Stats: ${PermissionHelper.hasUsageStatsPermission(context)}")
        android.util.Log.d("SettingsViewModel", "  Overlay: $overlayGranted")
        
        _permissionsState.value = PermissionsState(
            accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context),
            usageStatsGranted = PermissionHelper.hasUsageStatsPermission(context),
            batteryOptimizationDisabled = PermissionHelper.isBatteryOptimizationDisabled(context),
            notificationPermissionGranted = PermissionHelper.hasNotificationPermission(context),
            overlayPermissionGranted = overlayGranted
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
    
    fun openNotificationSettings() {
        PermissionHelper.openNotificationSettings(context)
    }
    
    fun openOverlaySettings() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    
    fun cleanOldLogs() {
        viewModelScope.launch {
            repository.cleanOldLogs(30)
        }
    }
}
