package com.appautomation.data.repository

import com.appautomation.data.model.DailyTask
import com.appautomation.data.model.TaskType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyTaskRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val tasksCollection = firestore.collection("daily_tasks")

    /**
     * Get tasks by date as Flow
     */
    fun getTasksByDate(date: String): Flow<List<DailyTask>> = callbackFlow {
        val listener = tasksCollection
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val tasks = snapshot?.documents?.map { doc ->
                    DailyTask.fromMap(doc.id, doc.data ?: emptyMap())
                }?.sortedBy { it.createdAt } ?: emptyList()
                
                trySend(tasks)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Get tasks grouped by type for a specific date
     */
    fun getTasksGroupedByType(date: String): Flow<Map<TaskType, List<DailyTask>>> = callbackFlow {
        val listener = tasksCollection
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val tasks = snapshot?.documents?.map { doc ->
                    DailyTask.fromMap(doc.id, doc.data ?: emptyMap())
                }?.sortedBy { it.createdAt } ?: emptyList()
                
                val grouped = tasks.groupBy { it.taskType }
                trySend(grouped)
            }
        
        awaitClose { listener.remove() }
    }

    // Write operations (add, update, delete) are disabled in the Android app.
    // Admin CRUD is handled via a separate web interface.


    // getAllTasks is not exposed in the Android app; admin uses web interface.

}
