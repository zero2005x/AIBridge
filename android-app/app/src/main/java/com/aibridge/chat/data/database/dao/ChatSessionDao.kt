package com.aibridge.chat.data.database.dao

import androidx.room.*
import com.aibridge.chat.data.database.entities.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    
    @Query("SELECT * FROM chat_sessions WHERE isActive = 1 ORDER BY lastModified DESC")
    fun getAllActiveSessions(): Flow<List<ChatSessionEntity>>
    
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)
    
    @Update
    suspend fun updateSession(session: ChatSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)
    
    @Query("UPDATE chat_sessions SET lastModified = :timestamp WHERE id = :sessionId")
    suspend fun updateLastModified(sessionId: String, timestamp: Long)
    
    @Query("UPDATE chat_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun deactivateSession(sessionId: String)
    
    @Query("DELETE FROM chat_sessions WHERE isActive = 0")
    suspend fun deleteInactiveSessions()
    
    @Query("SELECT COUNT(*) FROM chat_sessions WHERE isActive = 1")
    suspend fun getActiveSessionCount(): Int
}
