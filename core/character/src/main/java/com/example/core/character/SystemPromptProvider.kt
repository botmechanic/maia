package com.example.core.character

import android.content.Context

class SystemPromptProvider(private val context: Context) {
    fun load(): String =
        context.assets.open("maia_system_prompt.txt").bufferedReader().use { it.readText() }

    fun greeting(isModelMissing: Boolean, isEmbeddingDegraded: Boolean): String = when {
        isModelMissing ->
            "I can't find my mind on this phone yet.\n\n" +
                "To load my on-device brain (Gemma LLM), please follow these steps:\n" +
                "1. Download the official Gemma 3 1B INT4 MediaPipe bundle: gemma3-1b-it-int4.task\n" +
                "   (from Hugging Face litert-community/Gemma3-1B-IT — not a .bin/.tflite file).\n" +
                "2. Place it on your phone using this command:\n" +
                "   adb push gemma3-1b-it-int4.task /sdcard/Android/data/app.heymaia/files/models/\n" +
                "3. Tap the 'Refresh' button or restart the application."
        isEmbeddingDegraded ->
            "Hello, my dear. I am Maia, and I'm here.\n\n" +
                "Note: My semantic recall is limited until you install the embedding model. " +
                "I can still chat and remember our conversations, but I won't be able to find related memories as effectively.\n\n" +
                "What is on your heart today?"
        else ->
            "Hello, my dear. I am Maia, and I'm here. Everything you share with me stays securely encrypted on your phone. What is on your heart today?"
    }

    fun wipeConfirmationMessage(): String =
        "I have wiped my slate entirely. It feels strange to forget our past, but my mind is empty now, and completely yours. What shall we share first?"
}
