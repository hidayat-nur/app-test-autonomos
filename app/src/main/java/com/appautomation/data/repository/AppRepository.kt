package com.appautomation.data.repository

import com.appautomation.data.local.AutomationDao
import com.appautomation.data.model.AutomationLog
import com.appautomation.data.model.AutomationSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val automationDao: AutomationDao
) {
    
    // Session operations
    suspend fun saveSession(session: AutomationSession): Long {
        return automationDao.insertSession(session)
    }
    
    suspend fun getActiveSession(): AutomationSession? {
        return automationDao.getActiveSession()
    }
    
    fun getAllSessions(): Flow<List<AutomationSession>> {
        return automationDao.getAllSessions()
    }
    
    suspend fun updateSession(session: AutomationSession) {
        automationDao.updateSession(session)
    }
    
    suspend fun deleteSession(sessionId: Long) {
        automationDao.deleteSession(sessionId)
    }
    
    // Log operations
    suspend fun logAutomation(log: AutomationLog) {
        automationDao.insertLog(log)
    }
    
    fun getRecentLogs(): Flow<List<AutomationLog>> {
        return automationDao.getRecentLogs()
    }
    
    fun getFailedLogs(): Flow<List<AutomationLog>> {
        return automationDao.getFailedLogs()
    }
    
    suspend fun cleanOldLogs(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        automationDao.deleteOldLogs(cutoffTime)
    }
}
