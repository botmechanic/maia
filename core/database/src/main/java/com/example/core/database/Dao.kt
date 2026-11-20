package com.example.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodicDao {
    @Query("SELECT * FROM episodic_memories ORDER BY timestamp ASC")
    fun getAll(): Flow<List<EpisodicMemoryEntity>>

    @Query("SELECT * FROM episodic_memories ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<EpisodicMemoryEntity>

    @Query("SELECT * FROM episodic_memories WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<EpisodicMemoryEntity>

    @Query(
        "SELECT * FROM episodic_memories WHERE sessionId = :sessionId " +
            "AND (type = 'USER' OR type = 'MAIA') ORDER BY timestamp ASC"
    )
    suspend fun getChatTurnsForSession(sessionId: String): List<EpisodicMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: EpisodicMemoryEntity): Long

    @Delete
    suspend fun delete(memory: EpisodicMemoryEntity)

    @Query("DELETE FROM episodic_memories")
    suspend fun clearAll()
}

@Dao
interface SemanticDao {
    @Query("SELECT * FROM semantic_memories WHERE embedderId = :embedderId")
    suspend fun getAllByEmbedder(embedderId: String): List<SemanticMemoryEntity>

    @Query("SELECT * FROM semantic_memories")
    suspend fun getAll(): List<SemanticMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: SemanticMemoryEntity): Long

    @Query("DELETE FROM semantic_memories WHERE episodicMemoryId = :episodicId")
    suspend fun deleteByEpisodicId(episodicId: Long)

    @Query("DELETE FROM semantic_memories")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM semantic_memories WHERE embedderId = :embedderId")
    suspend fun countByEmbedder(embedderId: String): Int
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile_memories WHERE isSuperseded = 0 ORDER BY timestamp DESC")
    fun getAllActive(): Flow<List<ProfileMemoryEntity>>

    @Query("SELECT * FROM profile_memories ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ProfileMemoryEntity>>

    @Query("SELECT * FROM profile_memories WHERE id = :id")
    suspend fun getById(id: Long): ProfileMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: ProfileMemoryEntity): Long

    @Update
    suspend fun update(fact: ProfileMemoryEntity)

    @Delete
    suspend fun delete(fact: ProfileMemoryEntity)

    @Query("DELETE FROM profile_memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM profile_memories")
    suspend fun clearAll()
}
