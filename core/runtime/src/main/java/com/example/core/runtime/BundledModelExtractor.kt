package com.example.core.runtime

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Extracts on-device model files bundled via Google Play Asset Delivery (:model_pack)
 * from `assets/models/` into app-private `filesDir/models/` using atomic temp-file renames.
 *
 * When building from open-source GitHub clones without bundled weights, `assets/models/`
 * is empty and this returns null with zero overhead.
 */
object BundledModelExtractor {
    private const val TAG = "BundledModelExtractor"
    private const val ASSET_DIR = "models"

    fun ensureExtracted(context: Context, candidateFileNames: List<String>): File? {
        val bundledNames = try {
            context.assets.list(ASSET_DIR)?.toSet() ?: emptySet()
        } catch (e: Exception) {
            return null
        }

        val matchedName = candidateFileNames.firstOrNull { it in bundledNames } ?: return null
        val targetDir = File(context.filesDir, ASSET_DIR).apply { if (!exists()) mkdirs() }
        val targetFile = File(targetDir, matchedName)
        if (targetFile.exists() && targetFile.length() > 0L) {
            return targetFile
        }

        val tempFile = File(targetDir, "$matchedName.tmp")
        return try {
            Log.i(TAG, "Staging Play Asset Pack model '$matchedName' into internal storage...")
            context.assets.open("$ASSET_DIR/$matchedName").use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 256 * 1024)
                }
            }
            if (tempFile.renameTo(targetFile)) {
                Log.i(TAG, "Staged '$matchedName' (${targetFile.length() / (1024 * 1024)} MB).")
                targetFile
            } else {
                tempFile.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stage bundled model '$matchedName': ${e.message}", e)
            tempFile.delete()
            null
        }
    }
}

