package com.example.core.memory

import com.example.core.contract.ModelAccess
import com.example.core.contract.ModelRuntime
import kotlinx.coroutines.flow.Flow

class StreamingModelAccess(
    private val runtime: ModelRuntime
) : ModelAccess {
    override suspend fun generate(prompt: String): String {
        val sb = StringBuilder()
        runtime.generateResponseStreaming(prompt).collect { sb.append(it) }
        return sb.toString()
    }

    override suspend fun generateStreaming(prompt: String): Flow<String> =
        runtime.generateResponseStreaming(prompt)
}
