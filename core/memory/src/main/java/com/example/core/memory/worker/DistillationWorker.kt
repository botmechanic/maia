package com.example.core.memory.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.database.AppDatabase
import com.example.core.database.ProfileMemoryEntity
import com.example.core.memory.MaiaServices
import com.example.core.memory.toDomain
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class DistillationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DistillationWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder().setRequiresCharging(true).build()
            val request = OneTimeWorkRequestBuilder<DistillationWorker>()
                .setConstraints(constraints)
                .setInitialDelay(10, TimeUnit.SECONDS)
                .addTag("MaiaDistillation")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "MaiaDebouncedDistillation",
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Debounced distillation job queued.")
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder().setRequiresCharging(true).build()
            val request = PeriodicWorkRequestBuilder<DistillationWorker>(8, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag("MaiaPeriodicDistillation")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MaiaPeriodicDistillation",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Periodic distillation job scheduled.")
        }

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<DistillationWorker>()
                .addTag("MaiaImmediateDistillation")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "MaiaImmediateDistillation",
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Immediate distillation job queued.")
        }

        fun extractHeuristicFacts(events: List<com.example.core.model.EpisodicEvent>): List<ParsedFact> {
            val facts = mutableListOf<ParsedFact>()
            val nameRegex = Regex("""(?i)(?:my name is|i'm|i am)\s+([A-Z][a-z]+)""")
            val birthdayRegex = Regex("""(?i)(?:my birthday is|born on)(?:\s+on)?\s+([A-Za-z]+\s+\d{1,2}(?:st|nd|rd|th)?|\d{1,2}(?:st|nd|rd|th)?\s+of\s+[A-Za-z]+|\d{1,2}/\d{1,2}(?:/\d{2,4})?)""")
            val favoriteRegex = Regex("""(?i)my favorite\s+([a-zA-Z ]+?)\s+is\s+([^\.,!\n]+)""")
            val professionRegex = Regex("""(?i)(?:i work as(?: an?| the)?|my job is(?: an?| the)?|my profession is)\s+([^\.,!\n]+)""")
            val locationRegex = Regex("""(?i)(?:i live in|my home is in|i'm based in|i am based in)\s+([^\.,!\n]+)""")
            val moodRegex = Regex("""(?i)(?:i feel|i'm feeling|i am feeling)\s+(stressed|anxious|happy|excited|overwhelmed|tired|exhausted|peaceful|grateful|relieved)(?:\s+(?:about|with|because of)\s+([^\.,!\n]+))?""")

            for (event in events.filter { it.type == "USER" }) {
                nameRegex.find(event.content)?.groupValues?.getOrNull(1)?.let { name ->
                    facts.add(ParsedFact("User Name", name.trim(), event.id, "heuristic"))
                }
                birthdayRegex.find(event.content)?.groupValues?.getOrNull(1)?.let { bday ->
                    facts.add(ParsedFact("Birthday", bday.trim(), event.id, "heuristic"))
                }
                favoriteRegex.find(event.content)?.let { match ->
                    val category = match.groupValues.getOrNull(1)?.trim()?.replaceFirstChar(Char::uppercase)
                    val value = match.groupValues.getOrNull(2)?.trim()
                    if (!category.isNullOrBlank() && !value.isNullOrBlank()) {
                        facts.add(ParsedFact("Favorite $category", value, event.id, "heuristic"))
                    }
                }
                professionRegex.find(event.content)?.groupValues?.getOrNull(1)?.let { job ->
                    facts.add(ParsedFact("Profession", job.trim(), event.id, "heuristic"))
                }
                locationRegex.find(event.content)?.groupValues?.getOrNull(1)?.let { loc ->
                    facts.add(ParsedFact("Location", loc.trim(), event.id, "heuristic"))
                }
                moodRegex.find(event.content)?.let { match ->
                    val mood = match.groupValues.getOrNull(1)?.trim()?.replaceFirstChar(Char::uppercase)
                    val reason = match.groupValues.getOrNull(2)?.trim()
                    val moodValue = if (!reason.isNullOrBlank()) "$mood ($reason)" else mood
                    if (!moodValue.isNullOrBlank()) {
                        facts.add(ParsedFact("Recent Mood", moodValue, event.id, "heuristic"))
                    }
                }
            }
            return facts
        }
    }

    data class ParsedFact(
        val key: String,
        val value: String,
        val sourceId: Long,
        val source: String
    )

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background on-device memory distillation...")
        val db = AppDatabase.getDatabase(applicationContext)
        val episodicDao = db.episodicDao()
        val profileDao = db.profileDao()
        val memory = MaiaServices.memoryRepository
        val runtime = MaiaServices.modelRuntime

        try {
            val recentEvents = episodicDao.getRecent(20)
            if (recentEvents.isEmpty()) {
                Log.d(TAG, "No episodic memories to distill. Completing.")
                return Result.success()
            }

            val extractedFacts = mutableListOf<ParsedFact>()

            if (runtime != null && runtime.isReady.value) {
                try {
                    val conversationText = recentEvents.joinToString("\n") { "${it.type}: ${it.content}" }
                    val prompt = """
                        You are Maia's background cognitive summarizer. Analyze this local conversation and extract key facts about the user.
                        Focus on solid profile facts: name, family, hobby, birthday, job, home location, preferences, or recent reflections/mood.
                        Output exactly in format: Key: Value (one per line). Do not add any conversational text or prefixes.
                        
                        Conversation:
                        $conversationText
                        
                        Facts:
                    """.trimIndent()
                    val response = buildString {
                        runtime.generateResponseStreaming(prompt).collect { append(it) }
                    }
                    response.lines().forEach { line ->
                        if (line.contains(":")) {
                            val parts = line.split(":", limit = 2)
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            if (key.length > 2 && value.length > 2) {
                                val sourceId = recentEvents.firstOrNull {
                                    it.content.contains(value, ignoreCase = true)
                                }?.id ?: recentEvents.first().id
                                extractedFacts.add(ParsedFact(key, value, sourceId, "llm"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "LLM distillation failed. Falling back to heuristics: ${e.message}", e)
                }
            }

            if (extractedFacts.isEmpty()) {
                val domainEvents = recentEvents.map { it.toDomain() }
                extractedFacts.addAll(extractHeuristicFacts(domainEvents))
            }

            val activeFacts = profileDao.getAllActive().firstOrNull() ?: emptyList()
            for (fact in extractedFacts) {
                val existing = activeFacts.firstOrNull {
                    it.factKey.equals(fact.key, ignoreCase = true)
                }
                if (existing != null && existing.factValue.equals(fact.value, ignoreCase = true)) {
                    continue
                }
                if (memory != null) {
                    val newId = memory.addProfileMemory(
                        key = fact.key,
                        value = fact.value,
                        sourceEventIds = listOf(fact.sourceId),
                        moduleId = "chat",
                        source = fact.source
                    )
                    if (existing != null) {
                        memory.markProfileMemorySuperseded(existing.id, newId)
                    }
                } else {
                    val newId = profileDao.insert(
                        ProfileMemoryEntity(
                            factKey = fact.key,
                            factValue = fact.value,
                            sourceEventIdsString = fact.sourceId.toString(),
                            source = fact.source,
                            moduleId = "chat"
                        )
                    )
                    if (existing != null) {
                        profileDao.update(existing.copy(isSuperseded = true, supersededBy = newId))
                    }
                }
            }

            Log.d(TAG, "Distillation complete. Extracted ${extractedFacts.size} facts.")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Distillation failed: ${e.message}", e)
            return Result.retry()
        }
    }
}
