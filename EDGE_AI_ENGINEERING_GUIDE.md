# The On-Device AI Engineering Handbook
### A Comprehensive Architectural Guide & Codebase Reference for Edge AI Practitioners

> **Application Case Study:** Maia (`app.heymaia`)  
> **Core Stack:** Android (Kotlin, Jetpack Compose, Room, WorkManager)  
> **Edge ML Engines:** Google MediaPipe GenAI / LiteRT, MediaPipe TextEmbedder (USE-Lite)  
> **Security Stack:** SQLCipher (AES-256), Android Keystore (TEE / StrongBox)  
> **Hardware Target:** Google Pixel 7 (Tensor G2, 8GB RAM, Arm64-v8a)

---

## 1. The Edge AI Paradigm: Cloud vs. Edge

As an AI engineer moving to the edge, the entire system design philosophy shifts from **throughput & scaling** (cloud) to **resource bounds & invariants** (silicon):

| Architectural Dimension | Cloud AI (OpenAI, Anthropic, Bedrock) | Edge / On-Device AI (Maia, Mobile SLMs) |
| :--- | :--- | :--- |
| **Compute Constraints** | Massive GPU clusters (H100/A100) | Mobile SoC (Shared CPU, GPU, NPU/TPU) |
| **Memory (RAM) Limits** | Terabytes of unified host/device VRAM | 4GB - 12GB shared OS memory; OOM killer is ruthless |
| **Latency Jitter** | Network RTT (50ms - 400ms) + queueing | Deterministic local execution; zero network latency |
| **Thermal & Battery** | Datacenter liquid/air cooling; grid power | Passive dissipation; thermal throttling & battery drain |
| **Cost Dynamics** | $ / 1M tokens (OpEx scales linearly with users) | Zero marginal inference cost (CapEx / User hardware) |
| **Privacy & Trust** | Data transit, TLS, cloud SOC2, subpoena risk | Zero data transit; air-gapped cryptographic guarantees |
| **Failure Modes** | Network timeouts, rate limits, 503 errors | Process `SIGABRT` on token overflows, out-of-memory |

---

## 2. System Architecture & Modular Decoupling

Maia follows strict **Clean Architecture** multi-module boundaries. In edge AI engineering, decoupling domain contracts from runtime engines is vital because mobile ML runtimes evolve rapidly (e.g., TFLite $\rightarrow$ MediaPipe $\rightarrow$ LiteRT $\rightarrow$ ExecuTorch).

```
                      ┌───────────────────────────────────────────────┐
                      │                   :app                        │
                      │  (Composition Root, MainActivity, Manual DI)   │
                      └───────┬───────────────────────────────┬───────┘
                              │                               │
         ┌────────────────────┼───────────────────────────────┼───────────────────┐
         │                    │                               │                   │
         ▼                    ▼                               ▼                   │
┌─────────────────┐  ┌─────────────────┐             ┌─────────────────┐          │
│  :feature:chat  │  │:feature:memory  │             │:feature:settings│          │
└────────┬────────┘  └────────┬────────┘             └────────┬────────┘          │
         │                    │                               │                   │
         └────────────────────┼───────────────────────────────┘                   │
                              ▼                                                   ▼
                     ┌─────────────────┐                                 ┌─────────────────┐
                     │ :core:contract  │◄────────────────────────────────┤ :core:character │
                     │  (JVM Interfaces)                                 │ (Prompt Assets) │
                     └────────▲────────┘                                 └─────────────────┘
                              │
         ┌────────────────────┼───────────────────────────────┐
         │                    │                               │
┌────────┴────────┐  ┌────────┴────────┐             ┌────────┴────────┐
│  :core:runtime  │  │  :core:memory   │             │ :core:database  │
│ (MediaPipe/SLM) │  │(RAG / Workers)  │             │(SQLCipher/Keys) │
└────────┬────────┘  └────────┬────────┘             └────────┬────────┘
         │                    │                               │
         └────────────────────┼───────────────────────────────┘
                              ▼
                     ┌─────────────────┐
                     │   :core:model   │
                     │  (Pure Entities)│
                     └─────────────────┘
```

### Key Engineering Rules:
1. **`:core:model` & `:core:contract` have zero Android framework dependencies.** They are pure JVM modules. This allows running unit tests in milliseconds on host machines without Robolectric or an emulator.
2. **`:core:runtime` encapsulates native C++ / JNI libraries.** MediaPipe, LiteRT, or llama.cpp stay completely hidden behind JVM interfaces (`ModelRuntime`, `EmbeddingService`).
3. **Manual Dependency Injection (`MaiaAppContainer`).** Avoids reflection-heavy frameworks like Hilt/Dagger that add runtime overhead and binary bloat to edge binaries.

---

## 3. On-Device LLM Runtime Engineering

Located in: [`core/runtime/src/main/java/com/example/core/runtime/MediaPipeModelRuntime.kt`](core/runtime/src/main/java/com/example/core/runtime/MediaPipeModelRuntime.kt)

### 3.1 Model Quantization & Format
- **Model weights:** Quantized to **INT4** (4-bit integer weights, 16-bit float activations). This reduces a 1.2 billion parameter model (~2.4 GB in FP16) down to **~750 MB**, allowing it to fit into mobile RAM without triggering Android's Low Memory Killer (LMK).
- **Bundle Format:** MediaPipe requires a `.task` bundle (e.g. `gemma3-1b-it-int4.task`). Unlike bare `.tflite` or `.bin` files, the `.task` archive packages:
  1. Quantized model weights.
  2. The SentencePiece / BPE tokenizer vocabulary and merge tables.
  3. Pre-compiled execution subgraphs for prefill (prompt ingestion) and decode (autoregressive token generation).

### 3.2 The JNI `SIGABRT` Hazard (Hard Context Budgeting)
In cloud APIs, exceeding context length yields an HTTP 400 JSON error. In on-device C++ JNI engines, **exceeding `maxTokens` causes a native assertion failure and terminates the OS process with `SIGABRT`**.

```kotlin
// Defensive Token Budgeting in MediaPipeModelRuntime.kt
private const val MAX_TOKENS = 2048
private const val MAX_OUTPUT_TOKENS = 256
private const val OUTPUT_TOKEN_RESERVE = MAX_OUTPUT_TOKENS

private fun fitPromptToTokenBudget(inference: LlmInference, prompt: String): String {
    val maxInputTokens = MAX_TOKENS - OUTPUT_TOKEN_RESERVE
    var tokenCount = inference.sizeInTokens(prompt)
    if (tokenCount < maxInputTokens) return prompt

    // Iteratively drop oldest RAG / conversational context while keeping current user prompt
    var truncated = prompt
    while (truncated.length > 256 && tokenCount >= maxInputTokens) {
        val drop = (truncated.length * 0.20).toInt().coerceAtLeast(200)
        truncated = "[Earlier context trimmed]\n" + truncated.substring(drop)
        tokenCount = inference.sizeInTokens(truncated)
    }
    return truncated
}
```

### 3.3 KV Cache State Management (Stateless vs Stateful Sessions)
A critical bug in naive edge LLM implementations is reusing an inference session across multiple conversational turns. On device:
- The Key-Value (KV) cache grows monotonically in VRAM/RAM.
- Reusing dirty KV states causes prompt bleeding, context degradation, and memory leaks.
- **Maia's Pattern:** Keep the heavy `LlmInference` engine loaded in memory (avoiding cold-load penalty), but instantiate a fresh `LlmInferenceSession` for each generation request:

```kotlin
// Fresh session pattern: Zero KV state contamination
private fun runFreshSessionInference(prompt: String): Flow<String> = flow {
    val session = LlmInferenceSession.createFromOptions(
        inference, 
        sessionOptionsFor(selectedModelName.value)
    )
    try {
        session.addQueryChunk(prompt)
        val response = session.generateResponse()
        emit(response)
    } finally {
        session.close() // Reclaim session KV memory immediately
    }
}
```

### 3.4 Model Output Hygiene & Runaway Generation
Small models (1B–2B) have lower instruction-following fidelity than 70B+ models. In runtimes lacking native stop-sequence callbacks:
- The model often generates the user's next turn: `User: What do you mean?`
- It may parrot prompt delimiters: `<start_of_turn>model`
- **Solution:** [`ModelOutputHygiene.kt`](core/runtime/src/main/java/com/example/core/runtime/ModelOutputHygiene.kt) executes deterministic regex post-processing and cuts strings at the first phantom turn boundary.

---

## 4. On-Device Tri-Store RAG & Vector Search

Located in: [`core/memory/src/main/java/com/example/core/memory/RoomMemoryRepository.kt`](core/memory/src/main/java/com/example/core/memory/RoomMemoryRepository.kt)

Instead of bundling heavy vector databases (Milvus, Pinecone, or heavy C++ bindings), Maia implements an on-device **Tri-Store Architecture** directly atop encrypted SQLite:

```
                  ┌───────────────────────────────────────────────┐
                  │                 Tri-Store RAG                 │
                  └───────┬───────────────┬───────────────┬───────┘
                          │               │               │
                          ▼               ▼               ▼
                 ┌────────────────┐┌──────────────┐┌───────────────┐
                 │Episodic Memory ││Semantic Memory││Profile Memory │
                 │ (Raw dialogue  ││(USE-Lite 512d ││(Consolidated  │
                 │   time-series) ││ float vectors)││ user facts)   │
                 └────────────────┘└──────────────┘└───────────────┘
```

### 4.1 Local Vector Embeddings (Universal Sentence Encoder Lite)
- Runs via `MediaPipeTextEmbedder` with `universal_sentence_encoder.tflite` (~15 MB).
- Generates 512-dimensional dense float arrays.
- Configured with `setL2Normalize(true)`.

### 4.2 In-Database Cosine Similarity Math
When vectors are $L_2$-normalized ($||\mathbf{u}||_2 = 1$), cosine similarity simplifies directly to the dot product:

$$\text{CosineSimilarity}(\mathbf{u}, \mathbf{v}) = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\|_2 \|\mathbf{v}\|_2} = \sum_{i=1}^{512} u_i \cdot v_i$$

```kotlin
// In-Memory Dot Product over Encrypted SQL Records
override fun computeCosineSimilarity(vectorA: FloatArray, vectorB: FloatArray): Float {
    if (vectorA.size != vectorB.size) return 0f
    var dot = 0f
    for (i in vectorA.indices) {
        dot += vectorA[i] * vectorB[i]
    }
    return dot // Magnitude is 1.0 due to L2 normalization
}
```

### 4.3 The "Echo Hallucination" Filter (Crucial RAG Trap)
If an assistant retrieves its *own* previous responses as semantic memories, the model enters a recursive echo loop (e.g. reciting its own advice as if the user said it).
- **Invariant in `RoomMemoryRepository.kt`:**
```kotlin
// Mandatory filter: Never surface assistant-authored messages as user memory
if (episodic.type == "MAIA") return@mapNotNull null
```

---

## 5. Background Distillation & Battery-Aware Computing

Located in: [`core/memory/src/main/java/com/example/core/memory/worker/DistillationWorker.kt`](core/memory/src/main/java/com/example/core/memory/worker/DistillationWorker.kt)

Running neural inference in the background can rapidly overheat a mobile device and trigger OS battery-saver killing.

### 5.1 Two-Tier Memory Consolidation
```
User Message ──> [Tier 1: Heuristic Extraction (Regex)] ──> Instant Profile Facts (0% Battery)
                              │
                              ▼ (Accumulate 5+ turns)
                 [Tier 2: Background SLM Distillation] ──> Structured Semantic Facts (WorkManager)
```

1. **Tier 1 (Synchronous & Zero-Cost):** Fast deterministic regex heuristics extract high-confidence attributes (name, birthday, favorite coffee, mood) instantly upon insertion.
2. **Tier 2 (Asynchronous & Model-Driven):** Periodic `CoroutineWorker` batches recent episodic turns, passes them through a structured summarization prompt, and creates long-term profile records.

### 5.2 WorkManager Constraints
```kotlin
// Guarding Battery & Thermal Envelopes
val constraints = Constraints.Builder()
    .setRequiresCharging(true)       // Only consolidate when plugged in
    .setRequiresBatteryNotLow(true)  // Never execute in low-power states
    .build()

val request = PeriodicWorkRequestBuilder<DistillationWorker>(8, TimeUnit.HOURS)
    .setConstraints(constraints)
    .build()
```

---

## 6. Prompt Engineering & Prompt Templates for Gemma

Located in: [`core/contract/src/main/java/com/example/core/contract/GemmaChatFormatting.kt`](core/contract/src/main/java/com/example/core/contract/GemmaChatFormatting.kt)

Gemma models do **not** have a native `<start_of_turn>system` token. Using arbitrary system markers confuses the tokenizer and causes format degradation.

### Gemma-IT Native Turn Architecture:
```
<start_of_turn>user
[System Preamble / Persona / Temporal Anchors / RAG Context]

[First User Message]<end_of_turn>
<start_of_turn>model
[Assistant Response]<end_of_turn>
<start_of_turn>user
[Next User Message]<end_of_turn>
<start_of_turn>model
```

### Temporal Grounding for Edge AI
Mobile devices have access to high-accuracy local time, which cloud APIs often lack:
```kotlin
// Injected into system preamble at inference time
val now = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date())
// "Today is Wednesday, November 4, 2026 at 10:45 PM"
```
This enables temporal reasoning (*"What did I do yesterday?"*, *"My presentation is tomorrow morning"*) without needing external clock tools.

---

## 7. Air-Gapped Security & Hardware Enclaves

Located in: [`core/database/src/main/java/com/example/core/database/DatabaseKeyManager.kt`](core/database/src/main/java/com/example/core/database/DatabaseKeyManager.kt)

### 7.1 Hardware Envelope Encryption
1. **Master Key:** An AES-256 GCM key generated inside the **Android Keystore (Hardware-Backed TEE / StrongBox)**. Private keys never leave the secure hardware enclave.
2. **Database Passphrase:** A 256-bit cryptographically secure random key (`SecureRandom`) encrypted by the Master Key and stored in private SharedPreferences.
3. **Storage Engine:** SQLCipher encrypts all SQLite database pages on disk with 256-bit AES. If a phone is rooted or storage is imaged, the raw database is indistinguishable from random bytes.

### 7.2 Compile-Time & Invariant Security Testing
To guarantee an edge application never leaks data, security cannot rely on developer discipline—it must be enforced by **static analysis unit tests**:
- `PermissionAuditTest`: Inspects the merged `AndroidManifest.xml` and fails the CI build if `INTERNET` or `ACCESS_NETWORK_STATE` is introduced by any transitive dependency.
- `ModuleManifestInvariantsTest`: Asserts all feature and core modules declare `NetworkUse.None`.

---

## 8. Real-Time Telemetry & UX Metrics

Located in: [`feature/chat/src/main/java/com/example/feature/chat/ChatViewModel.kt`](feature/chat/src/main/java/com/example/feature/chat/ChatViewModel.kt)

### The Edge Telemetry Equation:
1. **Time To First Token (TTFT):** Measures prefill latency (prompt ingestion & tokenization).
   $$\text{TTFT} = T_{\text{first\_token}} - T_{\text{request\_sent}}$$
2. **Generation Throughput (Tokens per Second):**
   $$\text{Throughput} = \frac{N_{\text{output\_tokens}}}{T_{\text{completion}} - T_{\text{first\_token}}}$$

### Room Reactivity vs. Transient State Pattern
When streaming inference on Android:
- Keep token streaming in Compose in-memory state for 60fps smoothness.
- On completion, write the final record to Room.
- **Cache session telemetry by message content** so that when Room re-emits the persisted database entities, telemetry metrics are re-attached and do not vanish.

---

## 9. Edge AI Engineer's Study Guide & Checklist

When designing your own on-device AI applications in academic and industrial settings, use this verification checklist:

1. **Model Selection & Footprint:**
   - [ ] Is parameter count $\le 3\text{B}$ for standard mobile devices (4GB–8GB RAM)?
   - [ ] Is quantization (INT4 / AWQ / GGUF) verified on target hardware?
   - [ ] Are input token budgets strictly capped to prevent native runtime crashes?
2. **KV Cache & Memory Lifecycle:**
   - [ ] Is the inference session closed or reset after each generation?
   - [ ] Does the app handle Android `ComponentCallbacks2.onTrimMemory()` gracefully?
3. **RAG & Vector Storage:**
   - [ ] Are vector embeddings stored locally (no cloud roundtrip)?
   - [ ] Is similarity thresholding calibrated to prevent low-confidence noise injection?
   - [ ] Are assistant responses excluded from semantic memory retrieval?
4. **Thermal & Power Protection:**
   - [ ] Are heavy background summarizations constrained by `RequiresCharging` and `BatteryNotLow`?
   - [ ] Is CPU/GPU thread count capped to avoid overheating the SoC?
5. **Security & Privacy Proof:**
   - [ ] Are network permissions completely stripped from the application manifest?
   - [ ] Is the local vector store and dialogue history encrypted at rest using hardware-backed keys?

---

## 8. Production Edge Performance & Hardening Optimizations

Moving an on-device LLM app from a working prototype to a fast, Play Store–ready system requires solving mobile I/O, garbage-collection, memory-alignment, and lifecycle bottlenecks. Maia implements six concrete edge optimizations:

### 8.1 Compact Binary Vector `BLOB` Storage (`VectorCodec.kt`)
Located in: [`core/database/src/main/java/com/example/core/database/VectorCodec.kt`](core/database/src/main/java/com/example/core/database/VectorCodec.kt) and [`core/database/src/main/java/com/example/core/database/AppDatabase.kt`](core/database/src/main/java/com/example/core/database/AppDatabase.kt)
- **Problem:** Storing 512-dimensional embeddings as comma-separated ASCII strings (`vector.joinToString(",")`) consumes ~5.5 KB per message and forces `String.split(",")` + `toFloatOrNull()` allocations across every row on every user turn—causing GC pauses before LLM prefill even starts.
- **Optimization:** Room `MIGRATION_5_6` adds an `embeddingBlob BLOB` column and encodes vectors as packed little-endian IEEE-754 bytes (`ByteBuffer.allocate(512 * 4)` = **2,048 bytes**, ~2.7×–4× smaller with zero string parsing). `VectorCodec.decodeWithFallback()` transparently reads legacy v5 CSV rows so existing encrypted vaults upgrade with zero data loss.

### 8.2 Direct Internal Storage vs. Android FUSE `/sdcard` Overhead (`BundledModelExtractor.kt`)
Located in: [`core/runtime/src/main/java/com/example/core/runtime/BundledModelExtractor.kt`](core/runtime/src/main/java/com/example/core/runtime/BundledModelExtractor.kt)
- **Problem:** Loading a 528 MB `.task` model from `/sdcard/Android/data/...` routes file I/O through Android's userspace FUSE layer, adding latency to model `mmap` and weight reads—and requiring manual `adb push` for users.
- **Optimization:** Models bundled via Google Play Asset Delivery (`:model_pack`, `install-time`) are atomically staged (`.tmp` → rename) into app-private ext4/f2fs internal storage (`/data/user/0/app.heymaia/files/models/`). Both `MediaPipeModelRuntime` and `MediaPipeEmbeddingService` check internal storage directly, eliminating FUSE overhead while keeping `android.permission.INTERNET` stripped.

### 8.3 16 KB Page-Size Native ELF Alignment
- Upgraded SQLCipher to `net.zetetic:sqlcipher-android:4.6.1` and verified all `arm64-v8a` shared libraries (`libsqlcipher.so`, `libllm_inference_engine_jni.so`, `libmediapipe_tasks_text_jni.so`, `libandroidx.graphics.path.so`) are 16 KB ELF-aligned for modern Android kernels and Google Play requirements.

### 8.4 Off-Main-Thread Embedder Initialization
- `MediaPipeEmbeddingService.initializeEmbedder()` runs asynchronously on `Dispatchers.IO` inside [`MaiaAppContainer.kt`](app/src/main/java/com/example/di/MaiaAppContainer.kt), keeping cold-start UI rendering smooth and free of main-thread stalls.

### 8.5 Hardware RAM Guardrail & Graceful Vault Recovery
- **RAM Guardrail ([`DeviceCapabilityChecker.kt`](core/runtime/src/main/java/com/example/core/runtime/DeviceCapabilityChecker.kt)):** Inspects `ActivityManager.MemoryInfo.totalMem` to surface a calm compatibility notice on `< 5.5 GB` RAM devices and recommend `Gemma 4B` on `12 GB+` flagships.
- **Keystore Recovery ([`VaultRecoveryScreen.kt`](app/src/main/java/com/example/ui/screens/VaultRecoveryScreen.kt)):** Catches `VaultKeyException` cleanly on startup instead of crashing if hardware Keystore keys are invalidated, allowing a safe encrypted vault reset.

### 8.6 R8 Minification & JNI Keep Rules
- Release builds run with `isMinifyEnabled = true` and `isShrinkResources = true` backed by targeted MediaPipe, FlatBuffers, and SQLCipher keep rules in [`app/proguard-rules.pro`](app/proguard-rules.pro), shrinking the release binary while preserving native JNI entrypoints.

---

*Documented with care for your journey into Edge AI Engineering.*

