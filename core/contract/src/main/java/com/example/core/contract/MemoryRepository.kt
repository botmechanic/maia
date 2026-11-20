package com.example.core.contract

import com.example.core.model.ChatGenerationRequest
import com.example.core.model.ChatTurn
import com.example.core.model.EpisodicEvent
import com.example.core.model.ProfileFact
import com.example.core.model.SemanticHit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MemoryRepository {
    val allEpisodicMemories: Flow<List<EpisodicEvent>>
    val allActiveProfileMemories: Flow<List<ProfileFact>>
    val allProfileMemories: Flow<List<ProfileFact>>

    /** True when embedding model is unavailable; semantic recall is limited */
    val isEmbeddingDegraded: StateFlow<Boolean>

    suspend fun addEpisodicMemory(
        content: String,
        type: String,
        moduleId: String = "chat",
        sessionId: String = ""
    ): Long

    suspend fun addProfileMemory(
        key: String,
        value: String,
        sourceEventIds: List<Long>,
        moduleId: String = "chat",
        source: String = "heuristic"
    ): Long

    suspend fun markProfileMemorySuperseded(id: Long, supersededBy: Long)

    suspend fun querySemanticMemories(queryText: String, limit: Int = 3): List<SemanticHit>

    suspend fun getSourceEvents(fact: ProfileFact): List<EpisodicEvent>

    suspend fun deleteProfileFact(id: Long)

    suspend fun deleteEpisodicMemory(id: Long)

    suspend fun clearAllData()

    fun scheduleIndexRebuild()

    fun scheduleImmediateIndexRebuild()

    fun getKeySecurityLevelDescription(): String = "Hardware Keystore"

    /** Debounced background fact extraction after enough new episodic events. */
    fun scheduleDistillation()

    /** Immediate fact extraction without battery charging requirement (for manual trigger / demo). */
    fun scheduleImmediateDistillation()

    /** Prior USER/MAIA turns in [sessionId], oldest first. */
    suspend fun getSessionChatTurns(sessionId: String): List<ChatTurn>

    /**
     * Builds a structured chat request: system preamble (rules/profile/retrieved/examples)
     * + prior session turns (capped) + the new user message. No transcript labels.
     */
    suspend fun buildChatGenerationRequest(
        userQuery: String,
        systemPromptTemplate: String,
        sessionId: String,
        maxHistoryTurns: Int = 8
    ): ChatGenerationRequest

    /** @deprecated Prefer [buildChatGenerationRequest] for chat. Kept for distillation-style callers. */
    suspend fun assembleRagContext(userQuery: String, systemPromptTemplate: String): String

    /** Active profile rows for audit (key + source). */
    suspend fun listActiveProfileSummaries(): List<Pair<String, String>>

    /** Populates vault with a curated demo persona and conversational background. */
    suspend fun loadDemoPersona(personaName: String = "Keynote Researcher")

    fun writerFor(moduleId: String): MemoryWriter
}
