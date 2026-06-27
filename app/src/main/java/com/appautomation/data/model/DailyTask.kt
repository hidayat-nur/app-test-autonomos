package com.appautomation.data.model

/**
 * Task types for daily tasks
 */
enum class TaskType {
    DELETE_APP,   // Hapus app baru
    RATE_APP,     // Rating app
    TEST_APP,
    UPDATE_APP,
    NOTES
}

/**
 * Data class representing a daily task from Firestore
 */
data class DailyTask(
    val id: String = "",
    val date: String = "",           // Format: yyyy-MM-dd
    val appName: String = "",
    val packageName: String = "",
    val taskType: TaskType = TaskType.DELETE_APP,
    val playStoreUrl: String = "",   // Link Play Store
    val acceptUrl: String = "",      // Link accept (untuk test app)
    val credentials: String = "",    // Login akun "username:password" (opsional)
    val createdAt: Long = System.currentTimeMillis()
) {
    /** True when this task carries login credentials to show. */
    val hasCredentials: Boolean get() = credentials.isNotBlank()

    /** Username part of [credentials] (before the first ':'). */
    val credentialUsername: String
        get() = credentials.substringBefore(":", credentials).trim()

    /** Password part of [credentials] (after the first ':'), empty if none. */
    val credentialPassword: String
        get() = if (credentials.contains(":")) credentials.substringAfter(":").trim() else ""

    /**
     * Convert to Firestore map
     */
    fun toMap(): Map<String, Any> = mapOf(
        "date" to date,
        "appName" to appName,
        "packageName" to packageName,
        "taskType" to taskType.name,
        "playStoreUrl" to playStoreUrl,
        "acceptUrl" to acceptUrl,
        "credentials" to credentials,
        "createdAt" to createdAt
    )

    companion object {
        /**
         * Create DailyTask from Firestore document
         */
        fun fromMap(id: String, map: Map<String, Any?>): DailyTask {
            return DailyTask(
                id = id,
                date = map["date"] as? String ?: "",
                appName = map["appName"] as? String ?: "",
                packageName = map["packageName"] as? String ?: "",
                taskType = try {
                    TaskType.valueOf(map["taskType"] as? String ?: "DELETE_APP")
                } catch (e: Exception) {
                    TaskType.DELETE_APP
                },
                playStoreUrl = map["playStoreUrl"] as? String ?: "",
                acceptUrl = map["acceptUrl"] as? String ?: "",
                credentials = map["credentials"] as? String ?: "",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }
    }
}
