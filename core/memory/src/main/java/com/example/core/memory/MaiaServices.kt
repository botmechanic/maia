package com.example.core.memory

import com.example.core.contract.EmbeddingService
import com.example.core.contract.MemoryRepository
import com.example.core.contract.ModelRuntime

/**
 * Process-wide service locator for WorkManager workers.
 * Wired once from :app [MaiaAppContainer].
 */
object MaiaServices {
    @Volatile
    var modelRuntime: ModelRuntime? = null

    @Volatile
    var embeddingService: EmbeddingService? = null

    @Volatile
    var memoryRepository: MemoryRepository? = null

    fun requireModelRuntime(): ModelRuntime =
        modelRuntime ?: error("MaiaServices.modelRuntime not initialized")

    fun requireEmbeddingService(): EmbeddingService =
        embeddingService ?: error("MaiaServices.embeddingService not initialized")

    fun requireMemoryRepository(): MemoryRepository =
        memoryRepository ?: error("MaiaServices.memoryRepository not initialized")
}
