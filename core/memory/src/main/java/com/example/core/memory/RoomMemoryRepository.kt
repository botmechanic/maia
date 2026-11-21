package com.example.core.memory

import android.content.Context
import android.util.Log
import com.example.core.contract.EmbeddingService
import com.example.core.contract.MemoryRepository
import com.example.core.contract.MemoryWriter
import com.example.core.database.AppDatabase
import com.example.core.database.EpisodicMemoryEntity
import com.example.core.database.ProfileMemoryEntity
import com.example.core.database.SemanticMemoryEntity
import com.example.core.database.VectorCodec
import com.example.core.memory.worker.DistillationWorker
import com.example.core.memory.worker.RebuildIndexWorker
import com.example.core.contract.GemmaChatFormatting
import com.example.core.model.ChatGenerationRequest
import com.example.core.model.ChatRole
import com.example.core.model.ChatTurn
import com.example.core.model.EpisodicEvent
import com.example.core.model.ProfileFact
import com.example.core.model.SemanticHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RoomMemoryRepository(
    private val context: Context,
    private val embeddingService: EmbeddingService
) : MemoryRepository {

    private val db = AppDatabase.getDatabase(context)
    private val episodicDao = db.episodicDao()
    private val semanticDao = db.semanticDao()
    private val profileDao = db.profileDao()
    private val sharedPrefs = context.getSharedPreferences("maia_prefs", Context.MODE_PRIVATE)

    override val allEpisodicMemories: Flow<List<EpisodicEvent>> =
        episodicDao.getAll().map { list -> list.map { it.toDomain() } }

    override val allActiveProfileMemories: Flow<List<ProfileFact>> =
        profileDao.getAllActive().map { list -> list.map { it.toDomain() } }

    override val allProfileMemories: Flow<List<ProfileFact>> =
        profileDao.getAll().map { list -> list.map { it.toDomain() } }

    override val isEmbeddingDegraded: StateFlow<Boolean> = embeddingService.isModelMissing

    override suspend fun addEpisodicMemory(
        content: String,
        type: String,
        moduleId: String,
        sessionId: String
    ): Long =
        withContext(Dispatchers.IO) {
            val memory = EpisodicMemoryEntity(
                content = content,
                type = type,
                moduleId = moduleId,
                sessionId = sessionId
            )
            val episodicId = episodicDao.insert(memory)

            // Only USER / OBSERVATION enter the semantic index. MAIA replies stay in
            // the episodic log — not as "memories."
            if (type == "MAIA") return@withContext episodicId

            val embedderId = embeddingService.currentEmbedderId.value
            val dimension = embeddingService.currentDimension.value

            if (embedderId != null && dimension != null) {
                try {
                    val vector = embeddingService.getEmbedding(content)
                    if (vector != null) {
                        semanticDao.insert(
                            SemanticMemoryEntity(
                                episodicMemoryId = episodicId,
                                embeddingString = "",
                                embedderId = embedderId,
                                dimension = dimension,
                                moduleId = moduleId,
                                embeddingBlob = VectorCodec.encode(vector)
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to generate embedding for episodic ID: $episodicId", e)
                }
            }
            episodicId
        }

    override suspend fun addProfileMemory(
        key: String,
        value: String,
        sourceEventIds: List<Long>,
        moduleId: String,
        source: String
    ): Long = withContext(Dispatchers.IO) {
        profileDao.insert(
            ProfileMemoryEntity(
                factKey = key,
                factValue = value,
                sourceEventIdsString = sourceEventIds.joinToString(","),
                moduleId = moduleId,
                source = source
            )
        )
    }

    override suspend fun markProfileMemorySuperseded(id: Long, supersededBy: Long) =
        withContext(Dispatchers.IO) {
            val fact = profileDao.getById(id)
            if (fact != null) {
                profileDao.update(fact.copy(isSuperseded = true, supersededBy = supersededBy))
            }
        }

    override suspend fun querySemanticMemories(queryText: String, limit: Int): List<SemanticHit> =
        withContext(Dispatchers.IO) {
            val embedderId = embeddingService.currentEmbedderId.value ?: return@withContext emptyList()
            try {
                val queryVector = embeddingService.getEmbedding(queryText) ?: return@withContext emptyList()
                val allSemantics = semanticDao.getAllByEmbedder(embedderId)
                val minThreshold = sharedPrefs.getFloat("similarity_threshold", 0.35f)
                val topMatches = allSemantics.mapNotNull { semantic ->
                    val vector = VectorCodec.decodeWithFallback(semantic.embeddingBlob, semantic.embeddingString)
                    if (vector.size == queryVector.size) {
                        Pair(semantic.episodicMemoryId, embeddingService.computeCosineSimilarity(queryVector, vector))
                    } else null
                }
                    .filter { it.second >= minThreshold }
                    .sortedByDescending { it.second }

                if (topMatches.isEmpty()) return@withContext emptyList()
                val episodicMemories = episodicDao.getByIds(topMatches.map { it.first }).associateBy { it.id }
                topMatches.mapNotNull { (id, score) ->
                    val episodic = episodicMemories[id] ?: return@mapNotNull null
                    // Mandatory filter: never surface MAIA-authored text as retrieved memory.
                    if (episodic.type == "MAIA") return@mapNotNull null
                    SemanticHit(episodic.toDomain(), score)
                }.take(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error running semantic search: ${e.message}", e)
                emptyList()
            }
        }

    override suspend fun getSourceEvents(fact: ProfileFact): List<EpisodicEvent> =
        withContext(Dispatchers.IO) {
            if (fact.sourceEventIds.isEmpty()) emptyList()
            else episodicDao.getByIds(fact.sourceEventIds).map { it.toDomain() }
        }

    override suspend fun deleteProfileFact(id: Long) = withContext(Dispatchers.IO) {
        val factToDelete = profileDao.getById(id) ?: return@withContext
        val allFacts = profileDao.getAll().firstOrNull() ?: emptyList()
        val whoPointsToMe = allFacts.firstOrNull { it.supersededBy == id }

        if (factToDelete.isSuperseded) {
            if (whoPointsToMe != null) {
                profileDao.update(whoPointsToMe.copy(supersededBy = factToDelete.supersededBy))
            }
        } else if (whoPointsToMe != null) {
            profileDao.update(whoPointsToMe.copy(isSuperseded = false, supersededBy = null))
        }
        profileDao.deleteById(id)
    }

    override suspend fun deleteEpisodicMemory(id: Long) = withContext(Dispatchers.IO) {
        val memories = episodicDao.getByIds(listOf(id))
        if (memories.isNotEmpty()) {
            episodicDao.delete(memories[0])
            semanticDao.deleteByEpisodicId(id)
            val allProfileFacts = profileDao.getAll().firstOrNull() ?: emptyList()
            for (fact in allProfileFacts) {
                val sourceIds = fact.sourceEventIdsString.split(",").mapNotNull { it.trim().toLongOrNull() }
                if (sourceIds.contains(id)) {
                    val remainingIds = sourceIds.filter { it != id }
                    if (remainingIds.isEmpty()) {
                        deleteProfileFact(fact.id)
                    } else {
                        profileDao.update(fact.copy(sourceEventIdsString = remainingIds.joinToString(",")))
                    }
                }
            }
        }
    }

    override suspend fun clearAllData() = withContext(Dispatchers.IO) {
        episodicDao.clearAll()
        semanticDao.clearAll()
        profileDao.clearAll()
    }

    override fun scheduleIndexRebuild() = RebuildIndexWorker.schedule(context)

    override fun scheduleImmediateIndexRebuild() = RebuildIndexWorker.scheduleImmediate(context)

    override fun scheduleDistillation() = DistillationWorker.schedule(context)

    override fun scheduleImmediateDistillation() = DistillationWorker.scheduleImmediate(context)

    override suspend fun getSessionChatTurns(sessionId: String): List<com.example.core.model.ChatTurn> =
        withContext(Dispatchers.IO) {
            if (sessionId.isBlank()) return@withContext emptyList()
            episodicDao.getChatTurnsForSession(sessionId).map { entity ->
                com.example.core.model.ChatTurn(
                    role = if (entity.type == "MAIA") {
                        com.example.core.model.ChatRole.MODEL
                    } else {
                        com.example.core.model.ChatRole.USER
                    },
                    text = entity.content
                )
            }
        }

    override suspend fun buildChatGenerationRequest(
        userQuery: String,
        systemPromptTemplate: String,
        sessionId: String,
        maxHistoryTurns: Int
    ): com.example.core.model.ChatGenerationRequest = withContext(Dispatchers.IO) {
        val preamble = buildSystemPreamble(userQuery, systemPromptTemplate)
        val rawTurns = getSessionChatTurns(sessionId)
            // Drop the just-persisted current user message if present
            .let { turns ->
                if (turns.lastOrNull()?.role == com.example.core.model.ChatRole.USER &&
                    turns.last().text == userQuery
                ) {
                    turns.dropLast(1)
                } else turns
            }

        val prior = dampenHistoryTurns(rawTurns, maxTurns = maxHistoryTurns)

        com.example.core.model.ChatGenerationRequest(
            systemPreamble = preamble,
            history = prior,
            userMessage = userQuery
        )
    }

    override suspend fun assembleRagContext(userQuery: String, systemPromptTemplate: String): String {
        val request = buildChatGenerationRequest(
            userQuery = userQuery,
            systemPromptTemplate = systemPromptTemplate,
            sessionId = "",
            maxHistoryTurns = 0
        )
        return GemmaChatFormatting.buildGemmaPrompt(request)
    }

    override suspend fun listActiveProfileSummaries(): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            (profileDao.getAllActive().firstOrNull() ?: emptyList()).map { it.factKey to it.source }
        }

    override suspend fun loadDemoPersona(personaName: String) {
        val sessionId = "demo_session_${System.currentTimeMillis()}"
        val demoEpisodics = listOf(
            "I'm giving a research presentation tomorrow on privacy-first on-device AI companions.",
            "My name is Alex, and I work as an AI researcher at the university lab.",
            "My favorite coffee is an iced oat cortado with a dash of cinnamon.",
            "I've been feeling a bit nervous about the live audience demo, but excited to show real on-device speed.",
            "My partner Jordan is helping me review the slides tonight.",
            "I usually run 5k around the marina on Saturday mornings to clear my head."
        )

        val createdIds = mutableListOf<Long>()
        demoEpisodics.forEach { text ->
            val eventId = addEpisodicMemory(
                content = text,
                type = "USER",
                moduleId = "demo",
                sessionId = sessionId
            )
            createdIds.add(eventId)
        }

        val demoFacts = listOf(
            Triple("Name", "Alex", "heuristic"),
            Triple("Profession", "AI researcher at the university lab", "distillation"),
            Triple("Presentation Topic", "Privacy-first on-device AI companions tomorrow", "distillation"),
            Triple("Favorite Coffee", "Iced oat cortado with cinnamon", "heuristic"),
            Triple("Partner", "Jordan (helping review presentation slides)", "distillation"),
            Triple("Weekend Habit", "Runs 5k around the marina on Saturday mornings", "distillation"),
            Triple("Recent Mood", "Nervous but excited about the live demo presentation", "heuristic")
        )

        demoFacts.forEach { (key, value, src) ->
            addProfileMemory(
                key = key,
                value = value,
                sourceEventIds = createdIds,
                moduleId = "demo",
                source = src
            )
        }
    }

    private suspend fun buildSystemPreamble(userQuery: String, systemPromptTemplate: String): String {
        val activeFacts = (profileDao.getAllActive().firstOrNull() ?: emptyList()).map { it.toDomain() }
        val profileBlock = formatProfileBlock(activeFacts)

        val retrieved = querySemanticMemories(userQuery, limit = 3)
        var retrievedBlock = if (retrieved.isNotEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            retrieved.joinToString("\n") { hit ->
                GemmaChatFormatting.formatRetrievedMoment(
                    score = hit.score,
                    timeLabel = sdf.format(Date(hit.episodic.timestamp)),
                    content = hit.episodic.content
                )
            }
        } else EMPTY_SECTION_MARKER
        if (retrievedBlock.length > 1600) {
            retrievedBlock = retrievedBlock.take(1600) + "\n... [Retrieved memories truncated]"
        }

        return systemPromptTemplate
            .replace("{{PROFILE_FACTS}}", profileBlock)
            .replace("{{RETRIEVED_MEMORIES}}", retrievedBlock)
            .replace("{{CONVERSATION_HISTORY}}", "")
            .replace("{{USER_QUERY}}", userQuery)
            .trimEnd()
    }

    override fun writerFor(moduleId: String): MemoryWriter =
        ModuleScopedMemoryWriter(this, moduleId)

    override fun getKeySecurityLevelDescription(): String {
        return com.example.core.database.DatabaseKeyManager(context).getKeySecurityLevelDescription()
    }

    companion object {
        private const val TAG = "RoomMemoryRepository"
        /** Explicit empty marker — small models handle this better than a blank section. */
        const val EMPTY_SECTION_MARKER = "(nothing yet)"

        fun formatProfileBlock(
            facts: List<ProfileFact>,
            now: Date = Date()
        ): String {
            val sdfDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val dateHeader = "CURRENT DATE: ${sdfDate.format(now)}"
            val text = if (facts.isNotEmpty()) {
                "$dateHeader\n" + facts.joinToString("\n") { "${it.factKey}: ${it.factValue}" }
            } else dateHeader
            return if (text.length > 1200) {
                text.take(1200) + "\n... [Profile truncated]"
            } else text
        }

        fun dampenHistoryTurns(
            turns: List<com.example.core.model.ChatTurn>,
            maxTurns: Int = 8
        ): List<com.example.core.model.ChatTurn> {
            val capped = turns.takeLast(maxTurns)
            val modelIndices = capped.indices.filter { capped[it].role == com.example.core.model.ChatRole.MODEL }
            val modelIndicesToKeep = modelIndices.takeLast(2).toSet()

            return capped.mapIndexedNotNull { index, turn ->
                when (turn.role) {
                    com.example.core.model.ChatRole.USER -> turn
                    com.example.core.model.ChatRole.MODEL -> {
                        if (index in modelIndicesToKeep) {
                            val text = turn.text.trim()
                            val truncated = if (text.length > 200) text.take(200) + "..." else text
                            turn.copy(text = truncated)
                        } else null
                    }
                }
            }
        }
    }
}
