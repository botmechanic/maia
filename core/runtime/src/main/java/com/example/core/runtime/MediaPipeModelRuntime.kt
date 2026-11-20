package com.example.core.runtime

import com.example.core.contract.GemmaChatFormatting
import com.example.core.contract.ModelRuntime
import com.example.core.model.ChatGenerationRequest
import android.content.Context
import android.util.Log
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MediaPipeModelRuntime(private val context: Context) : ModelRuntime {

    companion object {
        private const val TAG = "MediaPipeModelRuntime"
        // maxTokens = input + output. Oversized prompts abort the process via JNI.
        private const val MAX_TOKENS = 2048
        /** Hard ceiling for chat replies — keeps runaway 1B output from flooding context. */
        private const val MAX_OUTPUT_TOKENS = 256
        private const val OUTPUT_TOKEN_RESERVE = MAX_OUTPUT_TOKENS
    }

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isModelLoading = MutableStateFlow(false)
    override val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    private val _isModelMissing = MutableStateFlow(false)
    override val isModelMissing: StateFlow<Boolean> = _isModelMissing.asStateFlow()

    private val _selectedModelName = MutableStateFlow("Gemma 1B")
    override val selectedModelName: StateFlow<String> = _selectedModelName.asStateFlow()

    private var llmInference: LlmInference? = null

    init {
        _selectedModelName.value = "Gemma 1B"
    }

    /** Gemma 1B: tighter sampling; 4B: slightly freer. */
    private fun sessionOptionsFor(
        modelName: String,
        temperatureOverride: Float? = null
    ): LlmInferenceSession.LlmInferenceSessionOptions {
        val is4b = modelName.contains("4B", ignoreCase = true)
        val temp = temperatureOverride ?: (if (is4b) 0.6f else 0.45f)
        return LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(temp)
            .setTopK(if (is4b) 40 else 32)
            .setRandomSeed(0)
            .build()
    }

    override fun getExpectedModelPath(modelName: String): String {
        val candidates = candidateFileNames(modelName)
        val searchDirs = modelSearchDirs()

        for (dir in searchDirs) {
            for (fileName in candidates) {
                val file = File(dir, fileName)
                if (file.exists()) {
                    return file.absolutePath
                }
            }
        }

        // Default to primary external path + preferred LiteRT filename for install instructions
        val preferredName = candidates.first()
        val primaryDir = searchDirs.firstOrNull() ?: File(context.filesDir, "models")
        return File(primaryDir, preferredName).absolutePath
    }

    private fun candidateFileNames(modelName: String): List<String> {
        return if (modelName == "Gemma 4B") {
            listOf(
                "gemma-4b-it-cpu-int4.task",
                "gemma3-4b-it-int4.task"
            )
        } else {
            // Prefer official LiteRT/MediaPipe Gemma 3 bundle, keep legacy Maia name as fallback
            listOf(
                "gemma3-1b-it-int4.task",
                "gemma-1b-it-cpu-int4.task"
            )
        }
    }

    private fun modelSearchDirs(): List<File> {
        val dirs = mutableListOf<File>()
        context.getExternalFilesDir("models")?.let { dirs.add(it) }
        val internal = File(context.filesDir, "models").also { if (!it.exists()) it.mkdirs() }
        dirs.add(internal)
        return dirs
    }

    private fun findMisnamedBinHint(modelName: String): String? {
        val binHints = if (modelName == "Gemma 4B") {
            listOf("gemma-4b-it-cpu-int4.bin", "gemma-4b-it-cpu-int4.tflite")
        } else {
            listOf(
                "gemma-1b-it-cpu-int4.bin",
                "gemma-1b-it-cpu-int4.tflite",
                "gemma3-1b-it-int4.bin"
            )
        }
        for (dir in modelSearchDirs()) {
            for (name in binHints) {
                val file = File(dir, name)
                if (file.exists()) {
                    return "Found incompatible model file '${file.name}'. MediaPipe requires an official .task bundle " +
                        "(with prefill/decode signatures + tokenizer), not .bin/.tflite. " +
                        "Download gemma3-1b-it-int4.task from Hugging Face litert-community/Gemma3-1B-IT."
                }
            }
        }
        return null
    }

    override suspend fun initializeModel(modelName: String): Boolean = withContext(Dispatchers.IO) {
        Log.d("MediaPipeModelRuntime", "=== Starting model initialization: $modelName ===")
        _selectedModelName.value = modelName
        _isModelLoading.value = true
        _isReady.value = false
        _isModelMissing.value = false

        val path = getExpectedModelPath(modelName)
        val file = File(path)

        Log.d("MediaPipeModelRuntime", "Expected model path: $path")
        Log.d(
            "MediaPipeModelRuntime",
            "File exists: ${file.exists()}, Size: ${if (file.exists()) file.length() / (1024 * 1024) else 0}MB"
        )

        if (!file.exists()) {
            val binHint = findMisnamedBinHint(modelName)
            if (binHint != null) {
                Log.e("MediaPipeModelRuntime", binHint)
            } else {
                Log.w("MediaPipeModelRuntime", "Model file not found at: $path.")
            }
            _isModelMissing.value = true
            _isModelLoading.value = false
            _isReady.value = false
            return@withContext false
        }

        if (!path.endsWith(".task")) {
            Log.e(
                "MediaPipeModelRuntime",
                "Model path must be a .task bundle for MediaPipe GenAI. Got: $path"
            )
            _isModelMissing.value = true
            _isModelLoading.value = false
            _isReady.value = false
            return@withContext false
        }

        try {
            // Close existing inference if any
            Log.d("MediaPipeModelRuntime", "Closing existing LLM inference (if any)...")
            llmInference?.close()
            llmInference = null

            Log.d(TAG, "Building LlmInference options...")
            // Prefer CPU for broad device compatibility (Pixel 7 and mid-range GPUs).
            // Temperature/topK moved to LlmInferenceSessionOptions in 0.10.29+.
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(MAX_TOKENS)
                .setMaxTopK(40)
                .setPreferredBackend(LlmInference.Backend.CPU)
                .build()

            Log.d(
                TAG,
                "Creating LlmInference from options... This may take 30-60 seconds for large models."
            )
            val startTime = System.currentTimeMillis()
            llmInference = LlmInference.createFromOptions(context, options)
            val loadTime = (System.currentTimeMillis() - startTime) / 1000

            Log.d(TAG, "✓ Model loaded successfully in ${loadTime}s")
            _isReady.value = true
            _isModelMissing.value = false
            _isModelLoading.value = false
            return@withContext true
        } catch (e: OutOfMemoryError) {
            Log.e(
                TAG,
                "!!! OUT OF MEMORY !!! Model is too large for this device. Consider using a smaller model.",
                e
            )
            _isModelMissing.value = true
            _isModelLoading.value = false
            _isReady.value = false
            return@withContext false
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("RET_CHECK", ignoreCase = true) ||
                message.contains("prefill", ignoreCase = true) ||
                message.contains("decode", ignoreCase = true)
            ) {
                Log.e(
                    TAG,
                    "Model/runtime signature mismatch while loading $path. " +
                        "Use MediaPipe tasks-genai 0.10.29+ with an official Gemma 3 .task file " +
                        "(e.g. gemma3-1b-it-int4.task from litert-community). " +
                        "Do not rename a .bin/.tflite file to .task. Error: ${e.message}",
                    e
                )
            } else {
                Log.e(TAG, "Error creating LlmInference: ${e.message}", e)
            }
            e.printStackTrace()
            _isModelMissing.value = true
            _isModelLoading.value = false
            _isReady.value = false
            return@withContext false
        }
    }

    /**
     * MediaPipe aborts the process (SIGABRT) if input tokens >= maxTokens.
     * Keep the end of the prompt (user query) and trim earlier RAG/system context.
     */
    private fun fitPromptToTokenBudget(inference: LlmInference, prompt: String): String {
        val maxInputTokens = MAX_TOKENS - OUTPUT_TOKEN_RESERVE
        return try {
            var tokenCount = inference.sizeInTokens(prompt)
            if (tokenCount < maxInputTokens) {
                Log.d(TAG, "Prompt fits token budget: $tokenCount / $maxInputTokens")
                return prompt
            }

            Log.w(TAG, "Prompt too long ($tokenCount tokens). Truncating to under $maxInputTokens...")
            var truncated = prompt
            while (truncated.length > 256 && tokenCount >= maxInputTokens) {
                val drop = (truncated.length * 0.20).toInt().coerceAtLeast(200)
                truncated = "[Earlier context trimmed]\n" + truncated.substring(drop)
                tokenCount = inference.sizeInTokens(truncated)
            }
            Log.d(TAG, "Truncated prompt to $tokenCount tokens (${truncated.length} chars)")
            truncated
        } catch (e: Exception) {
            // If tokenizer fails, fall back to a conservative character budget (~3 chars/token)
            val charBudget = maxInputTokens * 3
            Log.w(TAG, "sizeInTokens failed (${e.message}); using char budget $charBudget")
            if (prompt.length <= charBudget) prompt
            else "[Earlier context trimmed]\n" + prompt.takeLast(charBudget)
        }
    }

    /** Enforce ~256-token reply ceiling after generation (MediaPipe has no max-output knob). */
    private fun trimToOutputTokenCeiling(inference: LlmInference, response: String): String {
        return try {
            var tokenCount = inference.sizeInTokens(response)
            if (tokenCount <= MAX_OUTPUT_TOKENS) return response
            Log.w(TAG, "Response exceeded $MAX_OUTPUT_TOKENS tokens ($tokenCount). Trimming.")
            var trimmed = response
            while (trimmed.length > 32 && tokenCount > MAX_OUTPUT_TOKENS) {
                trimmed = trimmed.substring(0, (trimmed.length * 0.85).toInt()).trimEnd()
                val lastStop = trimmed.indexOfLast { it == '.' || it == '?' || it == '!' }
                if (lastStop > trimmed.length / 2) {
                    trimmed = trimmed.substring(0, lastStop + 1)
                }
                tokenCount = inference.sizeInTokens(trimmed)
            }
            trimmed
        } catch (e: Exception) {
            val charCap = MAX_OUTPUT_TOKENS * 4
            if (response.length <= charCap) response else response.take(charCap).trimEnd()
        }
    }

    /**
     * Await a Guava ListenableFuture without pulling in kotlinx-coroutines-guava.
     */
    private suspend fun <T> ListenableFuture<T>.await(): T =
        suspendCancellableCoroutine { cont ->
            addListener(
                {
                    try {
                        cont.resume(get())
                    } catch (e: Exception) {
                        cont.resumeWithException(e.cause ?: e)
                    }
                },
                { it.run() }
            )
        }

    override suspend fun generateChatStreaming(request: ChatGenerationRequest): Flow<String> {
        val prompt = GemmaChatFormatting.buildGemmaPrompt(request)
        val chunks = GemmaChatFormatting.buildChunkSequence(request)
        val requestId = System.identityHashCode(request)
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Chat chunk sequence (requestHash=$requestId, size=${chunks.size}):")
            chunks.forEachIndexed { idx, chunk ->
                Log.d(TAG, "  [$idx] ${chunk.role}: ${chunk.preview.take(30)}")
            }
        }
        val pastAssistantReplies = request.history
            .filter { it.role == com.example.core.model.ChatRole.MODEL }
            .takeLast(3)
            .map { it.text }
        return runFreshSessionInference(prompt, pastAssistantReplies)
    }

    override suspend fun generateResponseStreaming(prompt: String): Flow<String> =
        runFreshSessionInference(prompt)

    /**
     * One empty [LlmInferenceSession] per call. Engine stays loaded; session is never reused
     * across messages so KV state cannot accumulate contaminated turns.
     */
    private fun runFreshSessionInference(
        prompt: String,
        pastAssistantReplies: List<String> = emptyList()
    ): Flow<String> = flow {
        Log.d(TAG, "=== inference called ===")
        Log.d(TAG, "Prompt length: ${prompt.length} chars")
        Log.d(TAG, "llmInference: ${if (llmInference != null) "initialized" else "NULL"}")
        Log.d(TAG, "isModelMissing: ${_isModelMissing.value}")
        Log.d(TAG, "isReady: ${_isReady.value}")

        val inference = llmInference
        if (inference != null && !_isModelMissing.value) {
            try {
                val startTime = System.currentTimeMillis()
                val response = withContext(Dispatchers.IO) {
                    val fittedPrompt = fitPromptToTokenBudget(inference, prompt)
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Prompt bytes to model (${fittedPrompt.length} chars):\n$fittedPrompt")
                    }

                    suspend fun executePass(tempOverride: Float?): String {
                        var session: LlmInferenceSession? = null
                        return try {
                            session = LlmInferenceSession.createFromOptions(
                                inference,
                                sessionOptionsFor(_selectedModelName.value, tempOverride)
                            )
                            val sessionId = System.identityHashCode(session)
                            Log.d(
                                TAG,
                                "Fresh LlmInferenceSession id=$sessionId (per-turn, tempOverride=$tempOverride)"
                            )
                            session.addQueryChunk(fittedPrompt)
                            val raw = session.generateResponseAsync().await()
                            val cleaned = ModelOutputHygiene.sanitize(raw)
                            trimToOutputTokenCeiling(inference, cleaned)
                        } finally {
                            try {
                                session?.close()
                                Log.d(
                                    TAG,
                                    "Closed LlmInferenceSession id=${session?.let { System.identityHashCode(it) }}"
                                )
                            } catch (closeError: Exception) {
                                Log.w(TAG, "Session close failed: ${closeError.message}")
                            }
                        }
                    }

                    val firstPass = executePass(tempOverride = null)
                    if (pastAssistantReplies.isNotEmpty() &&
                        ModelOutputHygiene.isNearDuplicate(firstPass, pastAssistantReplies)
                    ) {
                        Log.w(
                            TAG,
                            "Anti-echo guard triggered: output prefix matches prior assistant reply. Retrying once at temp=0.8"
                        )
                        executePass(tempOverride = 0.8f)
                    } else {
                        firstPass
                    }
                }

                val inferenceTime = (System.currentTimeMillis() - startTime) / 1000
                Log.d(TAG, "✓ Inference complete in ${inferenceTime}s, len=${response.length}")

                if (response.isEmpty()) return@flow

                val chunks = response.split(" ")
                chunks.forEachIndexed { index, chunk ->
                    emit(if (index == chunks.lastIndex) chunk else "$chunk ")
                    delay(40)
                }
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "!!! OUT OF MEMORY during inference !!!", e)
                emit("I apologize, but I ran out of memory while thinking. The model may be too large for this device.")
            } catch (e: Exception) {
                Log.e(TAG, "Error during inference: ${e.message}", e)
                e.printStackTrace()
                emit(
                    "I am sorry, but my on-device brain (Gemma) encountered an inference issue: ${e.localizedMessage}. Please retry."
                )
            }
        } else {
            Log.w(
                TAG,
                "Cannot generate response: inference=${inference != null}, isModelMissing=${_isModelMissing.value}"
            )
            emit(
                "I can't find my mind on this phone yet. Please follow the setup instructions to load my on-device Gemma LLM model."
            )
        }
    }.flowOn(Dispatchers.Default)
}
