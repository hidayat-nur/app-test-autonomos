package com.appautomation.data.local

import androidx.room.*
import com.appautomation.data.model.AutomationLog
import com.appautomation.data.model.AutomationSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    
    // Session operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AutomationSession): Long
    
    @Query("SELECT * FROM automation_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): AutomationSession?
    
    @Query("SELECT * FROM automation_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<AutomationSession>>
    
    @Update
    suspend fun updateSession(session: AutomationSession)
    
    @Query("DELETE FROM automation_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
    
    // Log operations
    @Insert
    suspend fun insertLog(log: AutomationLog)
    
    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<AutomationLog>>
    
    @Query("SELECT * FROM automation_logs WHERE success = 0 ORDER BY timestamp DESC")
    fun getFailedLogs(): Flow<List<AutomationLog>>
    
    @Query("DELETE FROM automation_logs WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldLogs(beforeTimestamp: Long)
}
