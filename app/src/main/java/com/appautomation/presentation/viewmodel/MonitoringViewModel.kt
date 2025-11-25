package com.appautomation.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.appautomation.service.AutomationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MonitoringViewModel @Inject constructor(
    private val automationManager: AutomationManager
) : ViewModel() {
    
    val automationState: StateFlow<AutomationManager.AutomationState> = 
        automationManager.automationState
    
    fun pauseAutomation() {
        automationManager.pauseAutomation()
    }
    
    fun resumeAutomation() {
        automationManager.resumeAutomation()
    }
    
    fun stopAutomation() {
        automationManager.stopAutomation()
    }
    
    fun isRunning(): Boolean {
        return automationManager.isRunning()
    }
}
