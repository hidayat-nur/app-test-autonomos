package com.appautomation.presentation.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.filled.Star
import com.appautomation.service.AutomationAccessibilityService
import com.appautomation.service.AutomationForegroundService
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.appautomation.data.model.DailyTask
import com.appautomation.data.model.TaskType
import com.appautomation.presentation.viewmodel.DailyTaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Helper function to check if app is installed
fun isAppInstalled(packageManager: PackageManager, packageName: String): Boolean {
    return try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTaskScreen(
    viewModel: DailyTaskViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToMonitoring: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasksGrouped by viewModel.tasksGrouped.collectAsState()
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

    // Refresh counter to force recomposition when returning from uninstall
    var refreshKey by remember { mutableIntStateOf(0) }

    // Resolve installed-status ONCE per (task set, refresh) instead of calling
    // PackageManager for every item on every recomposition (was scroll jank).
    val installedStatus = remember(tasksGrouped, refreshKey) {
        tasksGrouped.values.flatten()
            .associate { it.packageName to isAppInstalled(context.packageManager, it.packageName) }
    }
    fun installed(pkg: String) = installedStatus[pkg] == true

    // Multi-select state for delete apps - moved up so lifecycle can access
    val selectedForDelete = remember { mutableStateListOf<String>() }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteQueue by remember { mutableStateOf<List<DailyTask>>(emptyList()) }

    // Lifecycle observer to refresh when resuming and continue delete queue
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Increment key to force recomposition and re-check installed status
                refreshKey++
                
                // Continue delete queue if there are pending deletes
                if (deleteQueue.isNotEmpty()) {
                    val nextTask = deleteQueue.first()
                    deleteQueue = deleteQueue.drop(1)
                    val intent = Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${nextTask.packageName}")
                    }
                    context.startActivity(intent)
                } else if (isDeleting) {
                    // Queue is empty, done deleting
                    isDeleting = false
                    selectedForDelete.clear()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Function to trigger sequential delete
    fun startSequentialDelete(tasks: List<DailyTask>) {
        if (tasks.isEmpty()) {
            isDeleting = false
            selectedForDelete.clear()
            return
        }
        isDeleting = true
        deleteQueue = tasks.drop(1)
        val firstTask = tasks.first()
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${firstTask.packageName}")
        }
        context.startActivity(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Tasks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        try {
                            cal.time = sdf.parse(selectedDate) ?: cal.time
                        } catch (e: Exception) { }
                        cal.add(Calendar.DAY_OF_MONTH, -1)
                        viewModel.setDate(sdf.format(cal.time))
                    }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous day")
                    }

                    // Format date for display: "2 Des 25"
                    val displayDate = remember(selectedDate) {
                        try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val date = inputFormat.parse(selectedDate)
                            val outputFormat = SimpleDateFormat("d MMM yy", Locale("id", "ID"))
                            outputFormat.format(date ?: Date())
                        } catch (e: Exception) {
                            selectedDate
                        }
                    }
                    
                    Text(
                        text = displayDate,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.titleSmall
                    )
                    IconButton(onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        try {
                            cal.time = sdf.parse(selectedDate) ?: cal.time
                        } catch (e: Exception) { }
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        viewModel.setDate(sdf.format(cal.time))
                    }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next day")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                item {
                    Text("Loading...", modifier = Modifier.padding(16.dp))
                }
            } else if (errorMessage != null) {
                item {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
            } else {
                // Section: Hapus App (DELETE_APP) with multi-select
                val deleteApps = tasksGrouped[TaskType.DELETE_APP] ?: emptyList()
                if (deleteApps.isNotEmpty()) {
                    // Header
                    item {
                        SectionHeader("Hapus App")
                    }
                    
                    // Select All + Delete Selected button in fixed row
                    item(key = "select_all_$refreshKey") {
                        val installedApps = deleteApps.filter { installed(it.packageName) }
                        Column {
                            // Select All row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedForDelete.size == installedApps.size && installedApps.isNotEmpty(),
                                    enabled = installedApps.isNotEmpty(),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedForDelete.clear()
                                            selectedForDelete.addAll(installedApps.map { it.id })
                                        } else {
                                            selectedForDelete.clear()
                                        }
                                    }
                                )
                                Text("Select All (${installedApps.size} installed)", fontWeight = FontWeight.Medium)
                            }
                            
                            // Delete Selected button - always visible when items selected
                            if (selectedForDelete.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        val tasksToDelete = deleteApps.filter { it.id in selectedForDelete }
                                        startSequentialDelete(tasksToDelete)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.width(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Selected (${selectedForDelete.size})")
                                }
                            }
                        }
                    }
                    
                    // Task items
                    items(deleteApps) { task ->
                        val isInstalled = installed(task.packageName)
                        DeleteTaskItemWithCheckbox(
                            task = task,
                            isSelected = task.id in selectedForDelete,
                            isInstalled = isInstalled,
                            onCheckedChange = { checked ->
                                if (checked && isInstalled) {
                                    selectedForDelete.add(task.id)
                                } else {
                                    selectedForDelete.remove(task.id)
                                }
                            },
                            onDeleteClick = {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:${task.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Section: Rating App (RATE_APP)
                val rateApps = tasksGrouped[TaskType.RATE_APP] ?: emptyList()
                if (rateApps.isNotEmpty()) {
                    item {
                        SectionHeader("Rating App")
                    }
                    item {
                        // Rate ALL apps in this list automatically, one by one.
                        Button(
                            onClick = {
                                if (!AutomationAccessibilityService.isServiceEnabled()) {
                                    Toast.makeText(
                                        context,
                                        "Aktifkan Accessibility Service terlebih dahulu",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (viewModel.startRatingAll(rateApps)) {
                                    val intent = Intent(context, AutomationForegroundService::class.java)
                                    context.startForegroundService(intent)
                                    onNavigateToMonitoring()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rating Semua (${rateApps.size})")
                        }
                    }
                    items(rateApps) { task ->
                        RateTaskItem(task) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=${task.packageName}")
                                    setPackage("com.android.vending")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://play.google.com/store/apps/details?id=${task.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Section: Test App Baru (TEST_APP)
                val testApps = tasksGrouped[TaskType.TEST_APP] ?: emptyList()
                if (testApps.isNotEmpty()) {
                    item {
                        SectionHeader("Test App Baru")
                    }
                    items(testApps) { task ->
                        TestTaskItem(
                            task = task,
                            onAcceptClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(task.acceptUrl)
                                }
                                context.startActivity(intent)
                            },
                            onAppClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse(task.playStoreUrl)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Section: Update App (UPDATE_APP)
                val updateApps = tasksGrouped[TaskType.UPDATE_APP] ?: emptyList()
                if (updateApps.isNotEmpty()) {
                    item {
                        SectionHeader("Update App")
                    }
                    items(updateApps) { task ->
                        UpdateTaskItem(task) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(task.playStoreUrl)
                            }
                            context.startActivity(intent)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Section: Notes (NOTES)
                val notes = tasksGrouped[TaskType.NOTES] ?: emptyList()
                if (notes.isNotEmpty()) {
                    item {
                        SectionHeader("Notes")
                    }
                    items(notes) { task ->
                        NoteTaskItem(task)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Empty state
                if (deleteApps.isEmpty() && rateApps.isEmpty() && testApps.isEmpty() && updateApps.isEmpty() && notes.isEmpty()) {
                    item {
                        Text(
                            text = "No tasks for this date",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Divider()
    }
}

@Composable
private fun DeleteTaskItemWithCheckbox(
    task: DailyTask,
    isSelected: Boolean,
    isInstalled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isInstalled) CardDefaults.cardColors()
                 else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val primaryText = if (isInstalled) MaterialTheme.colorScheme.onSurface
                          else MaterialTheme.colorScheme.onSurfaceVariant
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                enabled = isInstalled,
                onCheckedChange = onCheckedChange
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = primaryText
                )
                Text(
                    text = if (isInstalled) task.packageName else "${task.packageName} (Not installed)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onDeleteClick,
                enabled = isInstalled
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun RateTaskItem(task: DailyTask, onRateClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.appName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = task.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onRateClick) {
                Text("Play Store")
            }
        }
    }
}

@Composable
private fun TestTaskItem(task: DailyTask, onAcceptClick: () -> Unit, onAppClick: () -> Unit) {
    var showCredentials by remember { mutableStateOf(false) }
    val hasCred = task.hasCredentials

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(if (hasCred) Modifier.clickable { showCredentials = true } else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // Apps that need a login are tinted so they stand out in the list.
        colors = if (hasCred) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = task.appName, style = MaterialTheme.typography.titleSmall)
                    if (hasCred) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.width(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Login",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Text(
                    text = task.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasCred) {
                    Text(
                        text = "Ketuk kartu untuk lihat login",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Button(onClick = onAcceptClick) {
                Text("Accept")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAppClick) {
                Text("App")
            }
        }
    }

    if (showCredentials) {
        CredentialsDialog(task = task, onDismiss = { showCredentials = false })
    }
}

@Composable
private fun CredentialsDialog(task: DailyTask, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    fun copy(label: String, value: String) {
        clipboard.setText(AnnotatedString(value))
        Toast.makeText(context, "$label disalin", Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login • ${task.appName}") },
        text = {
            Column {
                CredentialRow(
                    label = "Username",
                    value = task.credentialUsername,
                    onCopy = { copy("Username", it) }
                )
                if (task.credentialPassword.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CredentialRow(
                        label = "Password",
                        value = task.credentialPassword,
                        onCopy = { copy("Password", it) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun CredentialRow(label: String, value: String, onCopy: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onCopy(value) }) { Text("Salin") }
        }
    }
}

@Composable
private fun UpdateTaskItem(task: DailyTask, onUpdateClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.appName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = task.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onUpdateClick) {
                Text("Update")
            }
        }
    }
}
@Composable
private fun NoteTaskItem(task: DailyTask) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = task.appName, // Content is stored in appName
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
