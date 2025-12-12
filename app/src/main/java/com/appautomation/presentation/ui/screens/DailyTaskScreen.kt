package com.appautomation.presentation.ui.screens

import android.content.Intent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.appautomation.data.model.DailyTask
import com.appautomation.data.model.TaskType
import com.appautomation.presentation.viewmodel.DailyTaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
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
    viewModel: DailyTaskViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasksGrouped by viewModel.tasksGrouped.collectAsState()
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

    // Refresh counter to force recomposition when returning from uninstall
    var refreshKey by remember { mutableIntStateOf(0) }

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
                actions = {
                    Button(onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        try {
                            cal.time = sdf.parse(selectedDate) ?: cal.time
                        } catch (e: Exception) { }
                        cal.add(Calendar.DAY_OF_MONTH, -1)
                        viewModel.setDate(sdf.format(cal.time))
                    }) {
                        Text("<")
                    }
                    Text(
                        text = selectedDate,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val cal = Calendar.getInstance()
                        try {
                            cal.time = sdf.parse(selectedDate) ?: cal.time
                        } catch (e: Exception) { }
                        cal.add(Calendar.DAY_OF_MONTH, 1)
                        viewModel.setDate(sdf.format(cal.time))
                    }) {
                        Text(">")
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
                        SectionHeader("Hapus App Baru")
                    }
                    
                    // Select All + Delete Selected button in fixed row
                    item(key = "select_all_$refreshKey") {
                        val installedApps = deleteApps.filter { isAppInstalled(context.packageManager, it.packageName) }
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("🗑️ Delete Selected (${selectedForDelete.size})")
                                }
                            }
                        }
                    }
                    
                    // Task items
                    items(deleteApps) { task ->
                        val isInstalled = isAppInstalled(context.packageManager, task.packageName)
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
                    items(rateApps) { task ->
                        RateTaskItem(task) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(task.playStoreUrl)
                            }
                            context.startActivity(intent)
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
                }

                // Empty state
                if (deleteApps.isEmpty() && rateApps.isEmpty() && testApps.isEmpty() && updateApps.isEmpty()) {
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
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
                 else CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.5f))
    ) {
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
                    fontWeight = FontWeight.Medium,
                    color = if (isInstalled) Color.Unspecified else Color.Gray
                )
                Text(
                    text = if (isInstalled) task.packageName else "${task.packageName} (Not installed)",
                    fontSize = 12.sp, 
                    color = if (isInstalled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
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
                Text(text = task.appName, fontWeight = FontWeight.Medium)
                Text(text = task.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onRateClick) {
                Text("Play Store")
            }
        }
    }
}

@Composable
private fun TestTaskItem(task: DailyTask, onAcceptClick: () -> Unit, onAppClick: () -> Unit) {
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
                Text(text = task.appName, fontWeight = FontWeight.Medium)
                Text(text = task.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(text = task.appName, fontWeight = FontWeight.Medium)
                Text(text = task.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onUpdateClick) {
                Text("Update")
            }
        }
    }
}
