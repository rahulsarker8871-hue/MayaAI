package com.maya.ai.memory
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fact: String
)
