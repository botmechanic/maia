# Maia Play Asset Delivery (`:model_pack`)

This Android Asset Pack delivers Maia's on-device AI models via **Google Play Asset Delivery (`install-time`)** so mainstream Play Store users can open Maia and chat immediately—without `adb push` and without adding `android.permission.INTERNET` to Maia's manifest.

## Building a Play Store Release Bundle (`.aab`)

1. Download the official quantized LiteRT / MediaPipe models:
   - `gemma3-1b-it-int4.task` (~555 MB) from [litert-community/Gemma3-1B-IT](https://huggingface.co/litert-community/Gemma3-1B-IT)
   - `universal_sentence_encoder.tflite` (~5.8 MB)
2. Copy them into:
   ```
   model_pack/src/main/assets/models/
   ```
   *(Model binaries are `.gitignore`d so they are never committed to Git.)*
3. Build the signed Android App Bundle:
   ```bash
   ./gradlew :app:bundleRelease
   ```
4. Upload `app/build/outputs/bundle/release/app-release.aab` to Google Play Console.

On first launch, `BundledModelExtractor` automatically detects models inside the Play Asset Pack and stages them into app-private storage for MediaPipe.

