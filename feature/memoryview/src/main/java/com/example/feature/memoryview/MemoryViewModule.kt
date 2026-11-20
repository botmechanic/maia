package com.example.feature.memoryview

import com.example.core.contract.ActivationResult
import com.example.core.contract.DataClaim
import com.example.core.contract.DataKind
import com.example.core.contract.LadderPosition
import com.example.core.contract.MaiaModule
import com.example.core.contract.ModuleContext
import com.example.core.contract.ModuleEntryPoint
import com.example.core.contract.ModuleManifest
import com.example.core.contract.NetworkUse
import com.example.core.contract.RetentionTier

class MemoryViewModule : MaiaModule {
    override val manifest = ModuleManifest(
        id = "memoryview",
        displayName = "What I Know About You",
        maiaDescription = "I'll show you the facts and memories I've kept about you on this phone, " +
            "and let you delete anything you want me to forget. I only look at what I've already stored — " +
            "I don't reach out for new sensors or accounts.",
        ladderCeiling = LadderPosition.SUGGEST,
        androidPermissions = emptyList(),
        specialAccess = emptyList(),
        observes = listOf(
            DataClaim(
                kind = DataKind.PROFILE_FACT,
                description = "Profile facts I've distilled from our chats",
                retention = RetentionTier.PERSISTENT_FACT
            ),
            DataClaim(
                kind = DataKind.CHAT_TEXT,
                description = "Past conversation turns stored in episodic memory",
                retention = RetentionTier.EXPIRING_30D
            )
        ),
        writes = emptyList(),
        networkUse = NetworkUse.None
    )

    override val entryPoints = listOf(
        ModuleEntryPoint(id = "memory_tab", label = "Memory", route = "memory")
    )

    override suspend fun onActivate(context: ModuleContext): ActivationResult =
        ActivationResult.Success

    override suspend fun onDeactivate(context: ModuleContext) = Unit
}
