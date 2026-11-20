package com.example.core.model

data class InferenceMetrics(
    val timeToFirstTokenMs: Long = 0,
    val totalGenerationTimeMs: Long = 0,
    val tokenCount: Int = 0,
    val tokensPerSecond: Float = 0f
)

data class ChatMessage(
    val id: Long,
    val sender: String, // "USER" | "MAIA"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val memoryUsed: String? = null,
    val sessionId: String = "",
    val metrics: InferenceMetrics? = null
)

data class EpisodicEvent(
    val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "USER", "MAIA", "OBSERVATION"
    val moduleId: String = "chat",
    val sessionId: String = ""
)

data class ProfileFact(
    val id: Long = 0,
    val factKey: String,
    val factValue: String,
    val sourceEventIds: List<Long> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isSuperseded: Boolean = false,
    val supersededBy: Long? = null,
    val source: String = "heuristic",
    val moduleId: String = "chat"
)

data class SemanticHit(
    val episodic: EpisodicEvent,
    val score: Float
)

enum class ChatRole { USER, MODEL }

/** One turn in a Gemma-it conversation (no screenplay labels). */
data class ChatTurn(
    val role: ChatRole,
    val text: String
)

/**
 * Structured chat generation request. History is prior turns in the *current*
 * conversation session only (capped by the caller, typically 8).
 */
data class ChatGenerationRequest(
    val systemPreamble: String,
    val history: List<ChatTurn>,
    val userMessage: String
)
