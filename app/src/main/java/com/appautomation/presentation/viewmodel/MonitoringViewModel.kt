package com.appautomation.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.appautomation.service.AutomationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MonitoringViewModel @Inject constructor(
    application: Application,
    private val automationManager: AutomationManager
) : AndroidViewModel(application) {
    
    private val prefs = application.getSharedPreferences("automation_prefs", Context.MODE_PRIVATE)
    
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
    
    fun moveToNextBatch() {
        val currentIndex = prefs.getInt("current_batch_index", 0)
        val batchSize = prefs.getInt("batch_size", 20)
        
        // Get selected apps count from preferences
        val selectedAppsJson = prefs.getString("selected_apps", "[]") ?: "[]"
        val selectedAppsCount = try {
            if (selectedAppsJson == "[]") 0 else selectedAppsJson.split(",").size
        } catch (e: Exception) {
            0
        }
        
        // Calculate total batches
        val totalBatches = if (selectedAppsCount == 0) 0 else (selectedAppsCount + batchSize - 1) / batchSize
        val nextIndex = currentIndex + 1
        
        // If next batch would exceed total, reset to 0 (cycle back)
        val finalIndex = if (nextIndex >= totalBatches) 0 else nextIndex
        
        prefs.edit().putInt("current_batch_index", finalIndex).apply()
    }
    
    fun isRunning(): Boolean {
        return automationManager.isRunning()
    }
}
