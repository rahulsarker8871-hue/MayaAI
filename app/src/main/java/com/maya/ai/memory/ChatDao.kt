package com.maya.ai.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_history ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatEntity>>

    @Insert
    suspend fun insertMessage(message: ChatEntity)

    @Query("DELETE FROM chat_history")
    suspend fun clearHistory()
}
