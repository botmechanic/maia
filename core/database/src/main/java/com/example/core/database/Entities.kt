package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodic_memories")
data class EpisodicMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "USER", "MAIA", "OBSERVATION"
    val moduleId: String = "chat",
    val sessionId: String = ""
)

@Entity(tableName = "semantic_memories")
data class SemanticMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val episodicMemoryId: Long,
    val embeddingString: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val embedderId: String,
    val dimension: Int,
    val moduleId: String = "chat",
    val embeddingBlob: ByteArray? = null
)

@Entity(tableName = "profile_memories")
data class ProfileMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val factKey: String,
    val factValue: String,
    val sourceEventIdsString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuperseded: Boolean = false,
    val supersededBy: Long? = null,
    val source: String = "heuristic",
    val moduleId: String = "chat"
)
