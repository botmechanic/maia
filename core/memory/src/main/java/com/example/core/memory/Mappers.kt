package com.example.core.memory

import com.example.core.database.EpisodicMemoryEntity
import com.example.core.database.ProfileMemoryEntity
import com.example.core.model.EpisodicEvent
import com.example.core.model.ProfileFact

internal fun EpisodicMemoryEntity.toDomain() = EpisodicEvent(
    id = id,
    content = content,
    timestamp = timestamp,
    type = type,
    moduleId = moduleId,
    sessionId = sessionId
)

internal fun ProfileMemoryEntity.toDomain() = ProfileFact(
    id = id,
    factKey = factKey,
    factValue = factValue,
    sourceEventIds = sourceEventIdsString.split(",").mapNotNull { it.trim().toLongOrNull() },
    timestamp = timestamp,
    isSuperseded = isSuperseded,
    supersededBy = supersededBy,
    source = source,
    moduleId = moduleId
)
