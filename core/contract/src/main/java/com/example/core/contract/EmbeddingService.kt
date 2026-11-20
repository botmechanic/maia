package com.example.core.contract

import kotlinx.coroutines.flow.StateFlow

interface EmbeddingService {
    val isModelMissing: StateFlow<Boolean>
    val currentEmbedderId: StateFlow<String?>
    val currentDimension: StateFlow<Int?>

    fun initializeEmbedder(): Boolean
    suspend fun getEmbedding(text: String): FloatArray?
    fun computeCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float
}
