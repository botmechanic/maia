package com.example

import android.app.Application
import android.os.Build
import android.util.Log
import com.example.di.MaiaAppContainer

/**
 * Custom Application class for Maia.
 * Ensures MaiaAppContainer (and therefore MaiaServices globals for Room,
 * ModelRuntime, and WorkManager DistillationWorker) is initialized on app startup,
 * while safely deferring native JNI loading during JVM Robolectric unit tests.
 */
class MaiaApplication : Application() {

    val container: MaiaAppContainer by lazy {
        MaiaAppContainer.get(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (!isRunningInRobolectric()) {
            try {
                // Eagerly initialize container and background services on real device
                MaiaAppContainer.get(this)
            } catch (e: Throwable) {
                Log.e(TAG, "Error initializing container on startup: ${e.message}", e)
            }
        }
    }

    private fun isRunningInRobolectric(): Boolean {
        return Build.FINGERPRINT == "robolectric"
    }

    companion object {
        private const val TAG = "MaiaApplication"
    }
}
