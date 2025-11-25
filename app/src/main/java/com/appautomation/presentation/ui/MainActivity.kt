package com.appautomation.presentation.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appautomation.presentation.ui.screens.AppSelectionScreen
import com.appautomation.presentation.ui.screens.MonitoringScreen
import com.appautomation.presentation.ui.screens.PermissionsScreen
import com.appautomation.presentation.ui.theme.AppAutomationTheme
import com.appautomation.presentation.viewmodel.SettingsViewModel
import com.appautomation.service.AutomationForegroundService
import com.appautomation.util.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppAutomationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val permissionsState by settingsViewModel.permissionsState.collectAsState()
    
    // Check if all permissions are granted
    val allPermissionsGranted = permissionsState.accessibilityEnabled && 
                                permissionsState.usageStatsGranted
    
    // Refresh permissions state when resuming
    LaunchedEffect(Unit) {
        settingsViewModel.checkPermissions()
    }
    
    NavHost(
        navController = navController,
        startDestination = if (allPermissionsGranted) "selection" else "permissions"
    ) {
        composable("permissions") {
            PermissionsScreen(
                viewModel = settingsViewModel,
                onPermissionsGranted = {
                    navController.navigate("selection") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        
        composable("selection") {
            AppSelectionScreen(
                onNavigateToMonitoring = {
                    navController.navigate("monitoring")
                }
            )
        }
        
        composable("monitoring") {
            MonitoringScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
