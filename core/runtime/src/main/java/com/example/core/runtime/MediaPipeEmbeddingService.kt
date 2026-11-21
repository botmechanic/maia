package com.example.core.runtime

import com.example.core.contract.EmbeddingService
import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.sqrt

class MediaPipeEmbeddingService(private val context: Context) : EmbeddingService {

    companion object {
        private const val TAG = "EmbeddingService"
        const val EMBEDDER_ID_USE_LITE = "use_lite_v2"
        const val USE_DIMENSION = 512
    }

    @Volatile
    private var textEmbedder: TextEmbedder? = null

    private val _isModelMissing = MutableStateFlow(true)
    override val isModelMissing: StateFlow<Boolean> = _isModelMissing.asStateFlow()

    private val _currentEmbedderId = MutableStateFlow<String?>(null)
    override val currentEmbedderId: StateFlow<String?> = _currentEmbedderId.asStateFlow()

    private val _currentDimension = MutableStateFlow<Int?>(null)
    override val currentDimension: StateFlow<Int?> = _currentDimension.asStateFlow()

    @Synchronized
    override fun initializeEmbedder(): Boolean {
        val extDir = context.getExternalFilesDir("models")
        val internalDir = File(context.filesDir, "models")
        val candidates = listOf(
            extDir?.let { File(it, "universal_sentence_encoder.tflite") },
            File(internalDir, "universal_sentence_encoder.tflite")
        )
        val modelFile = candidates.firstOrNull { it != null && it.exists() }
            ?: BundledModelExtractor.ensureExtracted(context, listOf("universal_sentence_encoder.tflite"))

        if (modelFile == null || !modelFile.exists()) {
            Log.w(TAG, "USE model file not found. Semantic memory is degraded.")
            _isModelMissing.value = true
            _currentEmbedderId.value = null
            _currentDimension.value = null
            textEmbedder = null
            return false
        }

        try {
            // Close existing embedder if any
            textEmbedder?.close()

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelFile.absolutePath)
                .build()

            val options = TextEmbedder.TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .setL2Normalize(true)
                .setQuantize(false)
                .build()

            textEmbedder = TextEmbedder.createFromOptions(context, options)
            _isModelMissing.value = false
            _currentEmbedderId.value = EMBEDDER_ID_USE_LITE
            _currentDimension.value = USE_DIMENSION
            Log.d(TAG, "TextEmbedder initialized with USE Lite (L2-normalized, ${USE_DIMENSION}d).")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextEmbedder: ${e.message}", e)
            _isModelMissing.value = true
            _currentEmbedderId.value = null
            _currentDimension.value = null
            textEmbedder = null
            return false
        }
    }

    override suspend fun getEmbedding(text: String): FloatArray? {
        val embedder = textEmbedder
        if (embedder == null) {
            // No fallback — return null when embedder unavailable
            return null
        }

        return try {
            val result = embedder.embed(text)
            val embeddings = result.embeddingResult().embeddings()
            if (embeddings.isNotEmpty()) {
                embeddings[0].floatEmbedding()
            } else {
                Log.w(TAG, "TextEmbedder returned empty embeddings for input.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding: ${e.message}", e)
            null
        }
    }

    override fun computeCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) {
            Log.e(TAG, "Cannot compute similarity: dimension mismatch (${vectorA.size} vs ${vectorB.size})")
            return 0.0f
        }

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0.0f) dotProduct / denominator else 0.0f
    }

    fun close() {
        textEmbedder?.close()
        textEmbedder = null
    }
}
