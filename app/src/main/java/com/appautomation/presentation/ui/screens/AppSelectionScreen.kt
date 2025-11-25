package com.appautomation.presentation.ui.screens

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.appautomation.data.model.AppInfo
import com.appautomation.presentation.viewmodel.AppSelectionViewModel
import com.appautomation.service.AutomationForegroundService
import com.appautomation.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    viewModel: AppSelectionViewModel = hiltViewModel(),
    onNavigateToMonitoring: () -> Unit
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val globalDuration by viewModel.globalDurationMinutes.collectAsState()
    val batchSize by viewModel.batchSize.collectAsState()
    val currentBatchIndex by viewModel.currentBatchIndex.collectAsState()
    val testedAppsToday by viewModel.testedAppsToday.collectAsState()
    
    var showGlobalDurationDialog by remember { mutableStateOf(false) }
    var showBatchSizeDialog by remember { mutableStateOf(false) }
    
    // Permission launcher for Android 13+ notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start automation
            if (viewModel.startAutomation()) {
                // Move to next batch for next time
                viewModel.moveToNextBatch()
                val intent = Intent(context, AutomationForegroundService::class.java)
                context.startForegroundService(intent)
                onNavigateToMonitoring()
            }
        } else {
            // Permission denied - still start but warn user
            if (viewModel.startAutomation()) {
                // Move to next batch for next time
                viewModel.moveToNextBatch()
                val intent = Intent(context, AutomationForegroundService::class.java)
                context.startForegroundService(intent)
                onNavigateToMonitoring()
            }
        }
    }
    
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isEmpty()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "BorderTech",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Select Apps",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showBatchSizeDialog = true }) {
                        Icon(Icons.Default.Settings, "Batch Settings")
                    }
                    if (selectedApps.isNotEmpty()) {
                        TextButton(onClick = { 
                            viewModel.clearAll()
                            viewModel.resetBatchIndex()
                        }) {
                            Text("Clear All")
                        }
                    }
                    IconButton(onClick = { viewModel.selectAll() }) {
                        Icon(Icons.Default.Done, "Select All")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedApps.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Global duration setting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Duration for all apps:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { showGlobalDurationDialog = true }) {
                                Text("$globalDuration min")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Edit, "Edit duration", modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Batch progress indicator
                        val totalBatches = viewModel.getTotalBatches()
                        val currentBatch = currentBatchIndex + 1
                        val startIndex = currentBatchIndex * batchSize
                        val endIndex = minOf(startIndex + batchSize, selectedApps.size)
                        val appsInThisBatch = endIndex - startIndex
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "📦 Batch $currentBatch of $totalBatches",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "$batchSize apps/batch",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Will test $appsInThisBatch apps (${startIndex + 1}-$endIndex of ${selectedApps.size})",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                // Progress bar
                                LinearProgressIndicator(
                                    progress = currentBatchIndex.toFloat() / totalBatches.toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "${selectedApps.size} apps selected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Start or Continue button
                        val buttonText = if (currentBatchIndex == 0) "Start Batch 1" else "Continue → Batch $currentBatch"
                        Button(
                            onClick = {
                                // Check and request notification permission for Android 13+
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    // For older versions, just start
                                    if (viewModel.startAutomation()) {
                                        // Move to next batch for next time
                                        viewModel.moveToNextBatch()
                                        val intent = Intent(context, AutomationForegroundService::class.java)
                                        context.startForegroundService(intent)
                                        onNavigateToMonitoring()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, "Start")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(buttonText)
                        }
                        
                        // Reset button if not on first batch
                        if (currentBatchIndex > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetBatchIndex() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.KeyboardArrowLeft, "Reset")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset to Batch 1")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppSelectionItem(
                            app = app,
                            isSelected = selectedApps.containsKey(app.packageName),
                            isTestedToday = testedAppsToday.contains(app.packageName),
                            onSelectionChanged = { isSelected ->
                                viewModel.toggleAppSelection(app, isSelected)
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
    
    // Global duration dialog
    if (showGlobalDurationDialog) {
        DurationPickerDialog(
            currentDuration = globalDuration,
            onDismiss = { showGlobalDurationDialog = false },
            onConfirm = { newDuration ->
                viewModel.updateGlobalDuration(newDuration)
                showGlobalDurationDialog = false
            }
        )
    }
    
    // Batch size dialog
    if (showBatchSizeDialog) {
        BatchSizePickerDialog(
            currentBatchSize = batchSize,
            onDismiss = { showBatchSizeDialog = false },
            onConfirm = { newSize ->
                viewModel.updateBatchSize(newSize)
                showBatchSizeDialog = false
            }
        )
    }
}

@Composable
fun AppSelectionItem(
    app: AppInfo,
    isSelected: Boolean,
    isTestedToday: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Box {
            app.icon?.let { drawable ->
                val bitmap: Bitmap = drawable.toBitmap()
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // Show checkmark badge if tested today
            if (isTestedToday) {
                Surface(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Done,
                        contentDescription = "Tested today",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    app.appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isTestedToday) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "✓ Tested",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                app.packageName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DurationPickerDialog(
    currentDuration: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var duration by remember { mutableStateOf(currentDuration) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Duration") },
        text = {
            Column {
                Text("Duration: $duration minutes")
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = Constants.MIN_DURATION_MINUTES.toFloat()..Constants.MAX_DURATION_MINUTES.toFloat(),
                    steps = Constants.MAX_DURATION_MINUTES - Constants.MIN_DURATION_MINUTES - 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${Constants.MIN_DURATION_MINUTES} min", fontSize = 12.sp)
                    Text("${Constants.MAX_DURATION_MINUTES} min", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(duration) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BatchSizePickerDialog(
    currentBatchSize: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var batchSize by remember { mutableStateOf(currentBatchSize) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch Size Settings") },
        text = {
            Column {
                Text(
                    "Set how many apps to test per batch. Smaller batches = more control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Apps per batch: $batchSize", fontWeight = FontWeight.Bold)
                Slider(
                    value = batchSize.toFloat(),
                    onValueChange = { batchSize = it.toInt() },
                    valueRange = Constants.MIN_BATCH_SIZE.toFloat()..Constants.MAX_BATCH_SIZE.toFloat(),
                    steps = Constants.MAX_BATCH_SIZE - Constants.MIN_BATCH_SIZE - 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${Constants.MIN_BATCH_SIZE}", fontSize = 12.sp)
                    Text("${Constants.MAX_BATCH_SIZE}", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "💡 Recommended: 15-25 apps for optimal control",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(batchSize) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

