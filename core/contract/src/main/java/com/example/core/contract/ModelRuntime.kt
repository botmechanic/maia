package com.example.core.contract

import com.example.core.model.ChatGenerationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ModelRuntime {
    val isReady: StateFlow<Boolean>
    val isModelLoading: StateFlow<Boolean>
    val isModelMissing: StateFlow<Boolean>
    val selectedModelName: StateFlow<String>

    suspend fun initializeModel(modelName: String): Boolean

    /** Native Gemma multi-turn chat (fresh MediaPipe session per call). */
    suspend fun generateChatStreaming(request: ChatGenerationRequest): Flow<String>

    /** Single-shot prompt (distillation / fallback). Still uses a fresh session. */
    suspend fun generateResponseStreaming(prompt: String): Flow<String>

    fun getExpectedModelPath(modelName: String): String
}
