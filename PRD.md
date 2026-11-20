# Product Requirements Document (PRD)
## Project Maia: Sovereign On-Device Personal AI Companion

| Document Metadata | Value |
| :--- | :--- |
| **Product Name** | **Maia** (`app.heymaia`) |
| **Version** | **v1.2 (Post-Modularization & Edge Feature Pack)** |
| **Status** | **Active / Open-Source Release** |
| **Platform** | Android 13+ (Target: Google Pixel 7 / Tensor G2, 8GB RAM, API 34) |
| **Author** | AI Engineering & Product Team |
| **Classification** | Open-Source Sovereign Edge AI System |

---

## 1. Executive Summary & Vision

### 1.1 The Problem
Every mainstream personal AI companion (OpenAI ChatGPT, Anthropic Claude, Inflection Pi) relies on centralized cloud infrastructure. Intimate personal disclosures—daily emotional reflections, interpersonal conflicts, health updates, and habits—are transmitted over the public internet, decrypted in remote data centers, and stored in centralized databases vulnerable to data breaches, corporate scraping, and governmental subpoenas. Furthermore, cloud latency, internet outages, and subscription API paywalls degrade the intimate continuity of an AI companion.

### 1.2 The Vision
**Maia** is an air-gapped, privacy-first personal AI companion built for mobile silicon. Maia runs **100% on-device**, never requests internet permissions, encrypts all memories using hardware-backed cryptographic keys, and features an intimate, mother-archetype persona that remembers your life through a local Tri-Store Retrieval-Augmented Generation (RAG) architecture.

> **Core Value Proposition:** *"She remembers. She protects. She's yours. Sovereign by design, private by math."*

---

## 2. Target Personas & Use Cases

### 2.1 Personas
1. **The Privacy-Conscious Professional:** Executives, researchers, lawyers, and engineers who cannot risk corporate or personal data leaking into cloud model training sets.
2. **The Intimate Journaler & Confidante Seeker:** Individuals who want a warm, empathetic listener to process thoughts, manage stress, and track personal goals without fear of surveillance.
3. **The Edge AI Enthusiast / Student:** Engineers looking for a transparent, mathematically verifiable on-device AI system that showcases local SLM inference, hardware Keystores, and local vector math.

### 2.2 Core User Stories
- *As a user in an offline environment (e.g., flight, remote location), I want to talk to Maia seamlessly with zero latency penalty or network connection errors.*
- *As a user sharing sensitive thoughts, I want cryptographic assurance that my conversations cannot be read even if my phone is inspected.*
- *As a user reflecting on my habits, I want Maia to remember past details (e.g., coffee preferences, colleague names, milestones) and visually explain how and why she remembers them.*

---

## 3. Current State Product Architecture & Specifications (v1.2)

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION / UI LAYER                         │
│   ChatScreen (STT/TTS)  │  MemoryScreen  │  MindMap 2D  │  Settings    │
├────────────────────────────────────────────────────────────────────────┤
│                         FEATURE & LOGIC LAYER                          │
│   ChatViewModel (Telemetry) │ MemoryViewModel │ SettingsViewModel       │
├────────────────────────────────────────────────────────────────────────┤
│                       DOMAIN CONTRACTS & CORE                          │
│     ModelRuntime     │    MemoryRepository    │    EmbeddingService    │
├────────────────────────────────────────────────────────────────────────┤
│                   LOCAL ENGINES & STORAGE (OFFLINE)                    │
│ MediaPipe GenAI (Gemma) │ Room + SQLCipher (AES-256) │ Keystore (TEE)  │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.1 Feature: On-Device Conversational Engine
- **Inference Stack:** Runs on Google MediaPipe GenAI / LiteRT executing quantized **INT4 Gemma 3 1B** (`gemma3-1b-it-int4.task`) or Gemma 4B.
- **Hardware Acceleration:** Native CPU / OpenCL / Vulkan execution optimized for Google Tensor G2 silicon.
- **Token Budgeting & Safety:**
  - Hard input/output token allocation (2048 token ceiling, 256 output token reserve).
  - Dynamic sliding-window context trimmer ([`fitPromptToTokenBudget`](core/runtime/src/main/java/com/example/core/runtime/MediaPipeModelRuntime.kt)) preventing native JNI `SIGABRT` crashes.
  - Stateless inference sessions: fresh `LlmInferenceSession` per turn to prevent KV cache contamination and memory bloat.
- **Output Hygiene:** Post-generation sanitization ([`ModelOutputHygiene.kt`](core/runtime/src/main/java/com/example/core/runtime/ModelOutputHygiene.kt)) stripping hallucinated multi-turn dialogues and turn delimiters.
- **Live Inference Telemetry:**
  - Real-time **Time to First Token (TTFT)** measurement in milliseconds.
  - Sustained throughput calculation in **tokens per second (tok/s)**.
  - Dual presentation: persistent pill under Maia's response bubble and dynamic header readout (`22.4 TOK/S • 160MS`).

### 3.2 Feature: Offline Hands-Free Voice Interaction
- **Speech-to-Text (STT):**
  - Integrated via Android's offline system recognizer (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH` with `EXTRA_PREFER_OFFLINE = true`).
  - **Zero extra Android permissions** required; audio stays on-device within the system service.
- **Text-to-Speech (TTS):**
  - Powered by native `android.speech.tts.TextToSpeech` via [`MaiaVoiceManager.kt`](feature/chat/src/main/java/com/example/feature/chat/MaiaVoiceManager.kt).
  - Tuned acoustic cadence: 0.96x speech rate, 0.92x pitch for a calm, grounded, motherly tone.
  - Automated regex Markdown/syntax stripper ensuring clean spoken text.
  - Interactive header auto-speak toggle and per-message "Listen" buttons.

### 3.3 Feature: Local Tri-Store RAG Pipeline
- **Episodic Memory:** Raw, chronological turn-by-turn dialogue logged with millisecond timestamps and session UUIDs.
- **Semantic Memory:**
  - On-device 512-dimensional vector embeddings generated via `universal_sentence_encoder.tflite` (USE-Lite) with $L_2$ normalization.
  - In-memory flat index cosine similarity (dot product math over encrypted SQL entities) executing in under 3ms.
  - Echo-prevention filter: strictly excludes assistant-authored messages (`type == "MAIA"`) from memory recall candidates.
- **Profile Memory:**
  - Key-value semantic facts about the user (e.g., `coffee_preference`, `sister_name`, `job_title`).
  - Provenance tracking linking every fact back to source episodic turn IDs (`sourceEventIds`).
  - Superseded fact tracking for automatic fact evolution.
- **Temporal Context Injection:** Real-world time, day, and date dynamically anchored into system preambles.

### 3.4 Feature: Memory Vault & 2D Mind Map Visualizer
- **Dual Mode UI:** Seamless header toggle between **List View** and **Mind Map**.
- **List View:**
  - Instant live keyword search.
  - Categorical filter pills (`All`, `Preferences`, `Habits`, `Relationships`, `Episodic`).
  - Fact deletion (GDPR Right to Be Forgotten) and manual fact addition.
  - Plaintext Markdown Vault Export for complete user data sovereignty.
- **Mind Map (Jetpack Compose Canvas):**
  - Pulsing central root node with breathing amber/terracotta gradient labeled with user persona.
  - Orbital satellite nodes clustered by semantic category (Warm Amber for Preferences, Coral Rose for Relationships, Sage for Habits, Lavender for Mood).
  - Multi-touch pinch-to-zoom and pan gestures with animated centering reset.
  - Floating Node Inspector Card displaying source badges (`DISTILLATION`, `HEURISTIC`, `MANUAL`) and an interactive **"Why I Know This"** event provenance viewer.

### 3.5 Feature: Background Distillation & Worker Lifecycle
- **Tier 1 (Instant Heuristic Extraction):** Synchronous regex parsing of key personal markers on message receipt (0% battery/compute overhead).
- **Tier 2 (Background SLM Distillation):** Scheduled periodic batch consolidation using Android `WorkManager` with strict hardware constraints (`setRequiresCharging(true)`, `setRequiresBatteryNotLow(true)`).

### 3.6 Feature: Zero-Trust Security & Audit
- **Manifest Invariants:** Build-time automated stripping of `android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, and Google Play Analytics.
- **Cryptographic Storage:** 256-bit AES page-level database encryption powered by SQLCipher.
- **Hardware Enclave:** Master encryption key generated and stored inside the **Android Keystore (Hardware TEE / StrongBox)**.
- **Live Security Audit Card in Settings:** Real-time display of SQLCipher DB file size, hardware Keystore status, and confirmed 0.00 KB network activity.
- **Demo Scenario Seeding:** One-tap demo persona loader (*Alex: Keynote Researcher*) pre-populating rich multi-category memories for instant testing.

---

## 4. Current Non-Functional Requirements & Performance SLAs

| Metric | Target SLA | Measured Performance (Pixel 7 / Tensor G2) |
| :--- | :--- | :--- |
| **Time to First Token (TTFT)** | $< 350\text{ ms}$ | **$140\text{ ms} - 220\text{ ms}$** |
| **Generation Throughput** | $> 15\text{ tok/s}$ | **$18.5 - 24.2\text{ tok/s}$** |
| **Vector Retrieval Latency** | $< 10\text{ ms}$ ($N < 5,000$ items) | **$1.8\text{ ms} - 3.4\text{ ms}$** |
| **Peak Memory Footprint (RSS)** | $< 1.8\text{ GB}$ | **$\sim 1.25\text{ GB}$** (within safe 8GB ceiling) |
| **Network Leakage** | **Absolute Zero (0.00 KB)** | **$0.00\text{ KB}$** (enforced by OS kernel & tests) |
| **Cold Startup Time** | $< 2.5\text{ s}$ | **$\sim 1.4\text{ s}$** |

---

## 5. Strategic Roadmap: Proposals & Ideas for Improvement

The following roadmap outlines prioritized feature proposals to evolve Maia from a research-grade demo into a market-defining consumer and enterprise edge AI platform.

```
┌────────────────────────────────────────────────────────────────────────┐
│                          FUTURE RELEASE ROADMAP                        │
├──────────────────┬──────────────────┬──────────────────────────────────┤
│    Phase 1: v1.5 │    Phase 2: v2.0 │    Phase 3: v2.5                 │
│  Security & Model│ Multi-Modal Edge │ Ecosystem & Context              │
├──────────────────┼──────────────────┼──────────────────────────────────┤
│ • Biometric Lock │ • On-Device VLM  │ • Health & Calendar Providers    │
│ • PII Sentinel   │   (PaliGemma)    │ • SIMD / Neon Vector Extension   │
│ • In-App Engine  │ • Audio Emotion  │ • Multi-Persona Adaptability     │
│   Download/Asset │   Detection      │ • Encrypted Cloud Backup (Zero-K)│
└──────────────────┴──────────────────┴──────────────────────────────────┘
```

### 5.1 Phase 1: Hardened Security & Frictionless Distribution (v1.5)

#### Proposal 1.1: Biometric Vault Lock (`BiometricPrompt`)
- **Problem:** If a user hands their unlocked phone to someone, their intimate chat history and mind map are exposed.
- **Solution:** Gate the app or specific memory categories behind Android's `BiometricPrompt` (Fingerprint / 3D Face Unlock).
- **Technical Implementation:** Integrate `androidx.biometric.BiometricPrompt` with `CryptoObject` derived from Keystore keys.

#### Proposal 1.2: On-Device PII & Sensitive Credential Sentinel
- **Problem:** Users may inadvertently type credit card numbers, passwords, recovery seeds, or sensitive medical codes into conversation.
- **Solution:** A client-side, zero-latency regex and Named Entity Recognition (NER) scanner that intercepts text *before* vectorization or database insertion.
- **UX:** Prompts the user: *"You mentioned a sensitive credential. Would you like Maia to redact this from permanent memory?"*

#### Proposal 1.3: Frictionless In-App Model Asset Delivery
- **Problem:** Models currently must be pushed via ADB commands (`adb push`) or pre-staged in development builds, which blocks non-technical users.
- **Solution:** Implement Google Play Feature Delivery or an in-app chunked resume-supported downloader that pulls verified `.task` and `.tflite` weights from signed mirrors with SHA-256 integrity verification.

---

### 5.2 Phase 2: Multimodal Edge Perception (v2.0)

#### Proposal 2.1: On-Device Vision-Language Memory (Edge VLM)
- **Problem:** Many personal memories are visual (receipts, whiteboard notes, books, places, physical prescriptions).
- **Solution:** Integrate a compact on-device vision model (e.g. **PaliGemma 3B** or **SmolVLM** quantized to INT4 via LiteRT).
- **Use Case:** User snaps a photo of a restaurant menu or a doctor's note; Maia extracts key facts, embeds the semantic description, and stores it in the Mind Map.

#### Proposal 2.2: Acoustic Prosody & Emotional Tone Detection
- **Problem:** STT converts voice to text, stripping emotional nuance (e.g. user sounding tired, stressed, or excited).
- **Solution:** Run a lightweight on-device audio classification model (e.g., YAMNet or an on-device emotion classifier) on the speech audio buffer to tag the interaction with an emotional state marker (e.g., `user_emotion: stressed`), allowing Maia to dynamically adapt her conversational tone.

---

### 5.3 Phase 3: Ambient Context & Vector Acceleration (v2.5)

#### Proposal 2.3: Zero-Permission Ambient Context Integrations
- **Problem:** The user has to manually type what they are doing.
- **Solution:** Integrate Android's local **Health Connect API** (sleep duration, step counts, resting heart rate) and **Local Calendar ContentProvider**.
- **Privacy Model:** Maia queries these local providers on-device only. Example interaction: *"You only slept 5 hours last night before today's keynote. Let's keep things easy today."*

#### Proposal 2.4: Hardware Vector Math Acceleration (SIMD / Arm NEON)
- **Problem:** As memory count exceeds 25,000 facts, in-memory Kotlin loops will experience micro-stutters during vector dot-product calculation.
- **Solution:** Implement a native C++ JNI vector math routine utilizing **Arm NEON intrinsic instructions** (`vdotq_f32`) or integrate `sqlite-vec` directly into the SQLCipher build to achieve 15x–30x vector similarity speedups.

#### Proposal 2.5: Zero-Knowledge Encrypted Multi-Device Sync
- **Problem:** If a user loses their phone, their companion and memories are lost forever.
- **Solution:** Client-side encrypted backup where the database is encrypted with a user-derived argon2id passphrase before uploading to private user-controlled cloud storage (e.g. Google Drive AppData folder or WebDAV). The cloud host never holds the decryption keys.

---

## 6. Success Metrics & KPIs

To measure the product's ongoing health and market impact:

1. **Inference Performance SLA:** $>98\%$ of responses begin streaming in $<250\text{ ms}$ on target hardware.
2. **Memory Retrieval Relevance:** High semantic precision ($>0.70$ cosine similarity match on recall turns).
3. **App Crash Rate:** $<0.01\%$ session aborts (zero native JNI `SIGABRT` crashes).
4. **Air-Gap Verification:** $100\%$ pass rate across automated permission audit and network invariant test suites.
5. **Memory Retention & Engagement:** High interaction density with the Mind Map Visualizer ($>40\%$ of sessions inspecting memory provenance).

---

*Maia — Redefining Personal AI Through On-Device Sovereignty.*

