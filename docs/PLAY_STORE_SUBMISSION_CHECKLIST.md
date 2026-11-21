# Google Play Store Submission Checklist — Maia (`app.heymaia`)

> **Target Track:** Closed Testing (14-day personal developer requirement with 12+ opted-in testers) → Production
> **Package ID:** `app.heymaia` | **Min SDK:** 24 | **Target / Compile SDK:** 36

---

## 1. Engineering & Binary Readiness (Completed in Code)

- [x] **Permanent Application ID:** Set to `app.heymaia` in [`app/build.gradle.kts`](../app/build.gradle.kts).
- [x] **Zero Network Permissions Verified:** `INTERNET`, `ACCESS_NETWORK_STATE`, and `READ_GSERVICES` stripped in [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) and enforced by [`PermissionAuditTest.kt`](../app/src/test/java/com/example/PermissionAuditTest.kt).
- [x] **16 KB Page-Size Native Alignment:** Upgraded SQLCipher to `4.6.1`; all `arm64-v8a` `.so` libraries (`libsqlcipher.so`, `libllm_inference_engine_jni.so`, `libmediapipe_tasks_text_jni.so`, `libandroidx.graphics.path.so`) verified 16 KB ELF-aligned.
- [x] **R8 Minification & Resource Shrinking:** `isMinifyEnabled = true` and `isShrinkResources = true` enabled with MediaPipe + SQLCipher keep rules in [`app/proguard-rules.pro`](../app/proguard-rules.pro).
- [x] **Non-Destructive Room Migrations:** Schema exported ([`6.json`](../core/database/schemas/com.example.core.database.AppDatabase/6.json)), `fallbackToDestructiveMigration` removed, explicit `MIGRATION_5_6` added for binary `BLOB` vector storage.
- [x] **Device-to-Device Backup Crash Protection:** `android:allowBackup="false"`, `data_extraction_rules.xml`, and `backup_rules.xml` configured + graceful [`VaultRecoveryScreen.kt`](../app/src/main/java/com/example/ui/screens/VaultRecoveryScreen.kt) fallback on `VaultKeyException`.
- [x] **Play Asset Delivery (`:model_pack`):** Install-time asset pack configured in [`model_pack/build.gradle.kts`](../model_pack/build.gradle.kts) with automatic first-run staging via [`BundledModelExtractor.kt`](../core/runtime/src/main/java/com/example/core/runtime/BundledModelExtractor.kt).
- [x] **AI-Generated Content Policy Compliance:**
  - Deterministic crisis intervention layer ([`CrisisIntervention.kt`](../feature/chat/src/main/java/com/example/feature/chat/CrisisIntervention.kt)) surfacing 988 / 741741 / international helplines.
  - Zero-network **"Report"** action on AI replies opening a pre-filled `mailto:` draft.
- [x] **Device RAM Guardrail:** [`DeviceCapabilityChecker.kt`](../core/runtime/src/main/java/com/example/core/runtime/DeviceCapabilityChecker.kt) surfaces a calm compatibility notice on `< 5.5 GB` RAM devices.
- [x] **Custom Adaptive Launcher Icon:** Portrait icon installed across `mdpi`–`xxxhdpi` + `mipmap-anydpi-v26` + `512 × 512` PNG at [`docs/images/play-store-icon-512.png`](images/play-store-icon-512.png).

---

## 2. Release Signing & `.aab` Build Steps (Run Locally Before Upload)

- [ ] **Generate an Upload Keystore (one-time):**
  ```bash
  keytool -genkey -v -keystore ~/maia-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
  ```
  *(Store `maia-upload-key.jks` and passwords in a password manager—never commit to Git.)*
- [ ] **Stage Model Weights into Play Asset Delivery (`:model_pack`):**
  Copy the official quantized models into `model_pack/src/main/assets/models/`:
  - `gemma3-1b-it-int4.task` (~555 MB)
  - `universal_sentence_encoder.tflite` (~5.8 MB)
- [ ] **Build the Signed Release App Bundle (`.aab`):**
  ```bash
  KEYSTORE_PATH=~/maia-upload-key.jks \
  STORE_PASSWORD="<store-password>" \
  KEY_PASSWORD="<key-password>" \
  ./gradlew clean testDebugUnitTest :app:bundleRelease
  ```
- [ ] **Verify Final Bundle Permissions & Smoke-Test on Pixel 7:**
  - Verify `app/build/outputs/bundle/release/app-release.aab` was generated.
  - Install and smoke-test the R8-minified release build on a physical device in **Airplane Mode** (verify chat, voice, Remembers, Mind Map, Biometric Vault Lock, and `.maiavault` export).

---

## 3. Store Listing Assets & Copy

| Asset | Requirement | Status / Location |
| :--- | :--- | :--- |
| **App Icon** | `512 × 512` PNG (≤ 1 MB) | Ready: [`docs/images/play-store-icon-512.png`](images/play-store-icon-512.png) |
| **Phone Screenshots** | Min 2, recommended 4–8 (`16:9` or `9:16`) | Ready: [`docs/images/screenshots/`](images/screenshots/) (`01_talk.png` – `04_privacy_settings.png`) |
| **Feature Graphic** | `1024 × 500` PNG/JPG | [ ] Export from your `maia` wordmark (`docs/images/maia-header.png` resized/framed to `1024 × 500`) |
| **Privacy Policy URL** | Live HTTPS page | [ ] Publish at `https://heymaia.app/privacy` |

### Suggested Play Store Copy
- **App Name:** `Maia — Private On-Device AI`
- **Short Description (≤ 80 chars):**
  `A warm AI companion that runs 100% on your phone. No cloud. No internet.`
- **Full Description Highlights:**
  - Runs quantized Gemma AI completely on-device—works in Airplane Mode.
  - Zero `INTERNET` permission in the app manifest (verifiable open-source code).
  - Encrypted SQLCipher memory vault protected by Android Hardware Keystore + Biometric Lock.
  - Inspectable memory: view *Why I Know This*, edit or forget any fact, and export AES-256-GCM `.maiavault` backups.
  - Honest voice disclosure: voice input uses Android's system speech recognizer (`EXTRA_PREFER_OFFLINE`); text chat and memory are 100% air-gapped.

---

## 4. Play Console Policy & Forms

- [ ] **Data Safety Form:**
  - *Does your app collect or share any of the required user data types?* → **No**
  - *Is all of the user data collected by your app encrypted in transit?* → N/A (no data collected or transmitted)
- [ ] **App Content Declarations:**
  - **Privacy Policy:** `https://heymaia.app/privacy`
  - **Ads:** No, my app does not contain ads.
  - **App Access:** All functionality is available without login/special access.
  - **Content Rating (IARC):** Complete questionnaire (recommend 13+ / Teen given open-ended AI conversation).
  - **AI-Generated Content Declaration:** Declare generative AI features; note in-app **"Report"** button on AI responses and deterministic crisis safety resources.
  - **Government Apps / Financial / Health:** No.
- [ ] **Closed Testing Track (Personal Developer Accounts):**
  - [ ] Recruit **12+ testers** (invite via Google Group or email list).
  - [ ] Ensure testers opt in and keep Maia installed for **14 continuous days**.
  - [ ] Collect feedback, resolve any crash/ANR reports in Play Console, then apply for **Production** access.

