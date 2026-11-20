package com.example.core.contract

import kotlinx.coroutines.flow.Flow

/**
 * Every Maia ability implements this. The contract makes privacy claims machine-readable:
 * the trust UI, activation flow, and permission audit are all rendered FROM this declaration.
 * If a module lies here, it shows in code review; nothing about data access is prose-only.
 */
interface MaiaModule {
    val manifest: ModuleManifest

    /** Called when the user activates the module. Must not touch any declared
     *  data source before this completes successfully. */
    suspend fun onActivate(context: ModuleContext): ActivationResult

    /** Full stop: unregister listeners, cancel workers. Data already written stays,
     *  governed by its retention tier and the user's delete controls. */
    suspend fun onDeactivate(context: ModuleContext)

    /** Optional entry point descriptors for navigation/UI surfaces this module adds. */
    val entryPoints: List<ModuleEntryPoint>
        get() = emptyList()
}

data class ModuleManifest(
    val id: String,
    val displayName: String,
    val maiaDescription: String,
    val ladderCeiling: LadderPosition,
    val androidPermissions: List<String>,
    val specialAccess: List<SpecialAccess>,
    val observes: List<DataClaim>,
    val writes: List<DataClaim>,
    val networkUse: NetworkUse,
)

enum class LadderPosition { SUGGEST, ACT_WITH_CONFIRMATION, ACT_AUTONOMOUSLY }

enum class SpecialAccess { NOTIFICATION_LISTENER, ACCESSIBILITY, DEVICE_ADMIN }

data class DataClaim(
    val kind: DataKind,
    val description: String,
    val retention: RetentionTier,
)

enum class DataKind {
    CHAT_TEXT,
    PROFILE_FACT,
    NOTIFICATION_CONTENT,
    CALENDAR_EVENT,
    LOCATION,
    WALLET_ACTIVITY
}

enum class RetentionTier {
    EPHEMERAL,
    EXPIRING_30D,
    PERSISTENT_FACT,
}

sealed interface NetworkUse {
    data object None : NetworkUse
    data class Declared(val destinations: List<String>, val reason: String) : NetworkUse
}

data class ModuleContext(
    val memory: MemoryWriter,
    val runtime: ModelAccess,
)

data class ModuleEntryPoint(
    val id: String,
    val label: String,
    val route: String,
)

sealed interface ActivationResult {
    data object Success : ActivationResult
    data class Failure(val reason: String) : ActivationResult
}

/**
 * Scoped writer: tags every event with the originating module id.
 */
interface MemoryWriter {
    val moduleId: String
    suspend fun addEpisodic(content: String, type: String): Long
    suspend fun addProfileFact(key: String, value: String, sourceEventIds: List<Long>): Long
}

/**
 * Inference access; no direct file/db handles.
 */
interface ModelAccess {
    suspend fun generate(prompt: String): String
    suspend fun generateStreaming(prompt: String): Flow<String>
}

class ModuleRegistry(
    private val modules: List<MaiaModule>
) {
    fun all(): List<MaiaModule> = modules

    fun find(id: String): MaiaModule? = modules.find { it.manifest.id == id }
}
