package com.appautomation.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.appautomation.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onPermissionsGranted: () -> Unit
) {
    val permissionsState by viewModel.permissionsState.collectAsState()
    
    // Auto-navigate when all permissions granted
    LaunchedEffect(permissionsState) {
        if (permissionsState.accessibilityEnabled && permissionsState.usageStatsGranted) {
            onPermissionsGranted()
        }
    }
    
    // Refresh permissions when screen comes back to focus
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val job = scope.launch {
            while (isActive) {
                delay(1000)
                viewModel.checkPermissions()
            }
        }
        onDispose {
            job.cancel()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Required Permissions") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "App Automation",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Please grant the following permissions to enable automatic app launching and monitoring",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Accessibility Service Permission
            PermissionCard(
                title = "Accessibility Service",
                description = "Required to control and interact with other apps automatically",
                icon = Icons.Default.Accessibility,
                isGranted = permissionsState.accessibilityEnabled,
                onClick = { viewModel.openAccessibilitySettings() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Usage Stats Permission
            PermissionCard(
                title = "Usage Stats Access",
                description = "Required to detect which app is currently running",
                icon = Icons.Default.Analytics,
                isGranted = permissionsState.usageStatsGranted,
                onClick = { viewModel.openUsageStatsSettings() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Battery Optimization
            PermissionCard(
                title = "Battery Optimization",
                description = "Recommended: Disable to ensure automation runs smoothly",
                icon = Icons.Default.BatteryChargingFull,
                isGranted = permissionsState.batteryOptimizationDisabled,
                onClick = { viewModel.requestBatteryOptimizationExemption() },
                isOptional = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (permissionsState.accessibilityEnabled && permissionsState.usageStatsGranted) {
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Please grant required permissions above",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onClick: () -> Unit,
    isOptional: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isGranted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isGranted) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (isOptional) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(Optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGranted) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isGranted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            if (!isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}
