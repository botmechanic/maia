package com.example.core.memory

import com.example.core.contract.MemoryRepository
import com.example.core.contract.MemoryWriter

class ModuleScopedMemoryWriter(
    private val repository: MemoryRepository,
    override val moduleId: String
) : MemoryWriter {
    override suspend fun addEpisodic(content: String, type: String): Long =
        repository.addEpisodicMemory(content, type, moduleId)

    override suspend fun addProfileFact(key: String, value: String, sourceEventIds: List<Long>): Long =
        repository.addProfileMemory(key, value, sourceEventIds, moduleId)
}
