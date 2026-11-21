package com.example.core.memory.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.database.AppDatabase
import com.example.core.database.SemanticMemoryEntity
import com.example.core.memory.MaiaServices
import java.util.concurrent.TimeUnit

class RebuildIndexWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RebuildIndexWorker"
        private const val WORK_NAME = "MaiaRebuildSemanticIndex"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiresCharging(true).build()
            val request = OneTimeWorkRequestBuilder<RebuildIndexWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .addTag("MaiaRebuildIndex")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Semantic index rebuild job scheduled (requires charging).")
        }

        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder().setRequiresCharging(true).build()
            val request = OneTimeWorkRequestBuilder<RebuildIndexWorker>()
                .setConstraints(constraints)
                .addTag("MaiaRebuildIndex")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Immediate semantic index rebuild job scheduled.")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting semantic index rebuild...")
        val db = AppDatabase.getDatabase(applicationContext)
        val episodicDao = db.episodicDao()
        val semanticDao = db.semanticDao()
        val embeddingService = MaiaServices.requireEmbeddingService()

        if (!embeddingService.initializeEmbedder()) {
            Log.w(TAG, "Embedding model not available. Cannot rebuild index.")
            return Result.failure()
        }

        val embedderId = embeddingService.currentEmbedderId.value
        val dimension = embeddingService.currentDimension.value
        if (embedderId == null || dimension == null) {
            return Result.failure()
        }

        return try {
            semanticDao.clearAll()
            val episodics = episodicDao.getRecent(Int.MAX_VALUE)
            var successCount = 0
            for (episodic in episodics) {
                if (episodic.type == "MAIA") continue
                val vector = embeddingService.getEmbedding(episodic.content)
                if (vector != null) {
                    semanticDao.insert(
                        SemanticMemoryEntity(
                            episodicMemoryId = episodic.id,
                            embeddingString = "",
                            embedderId = embedderId,
                            dimension = dimension,
                            moduleId = episodic.moduleId,
                            embeddingBlob = com.example.core.database.VectorCodec.encode(vector)
                        )
                    )
                    successCount++
                }
            }
            Log.d(TAG, "Rebuild complete: $successCount embedded.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during index rebuild: ${e.message}", e)
            Result.failure()
        }
    }
}
