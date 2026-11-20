package com.example.feature.chat

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

class ChatModule : MaiaModule {
    override val manifest = ModuleManifest(
        id = "chat",
        displayName = "Chat",
        maiaDescription = "I'll listen to what you share with me, keep our conversation encrypted on this phone, " +
            "and gently remember the facts you tell me about your life. Nothing leaves the device.",
        ladderCeiling = LadderPosition.SUGGEST,
        androidPermissions = emptyList(),
        specialAccess = emptyList(),
        observes = listOf(
            DataClaim(
                kind = DataKind.CHAT_TEXT,
                description = "The words you type in our conversation",
                retention = RetentionTier.EXPIRING_30D
            )
        ),
        writes = listOf(
            DataClaim(
                kind = DataKind.CHAT_TEXT,
                description = "Episodic log of our exchanges",
                retention = RetentionTier.EXPIRING_30D
            ),
            DataClaim(
                kind = DataKind.PROFILE_FACT,
                description = "Distilled facts about you (name, people you mention, preferences)",
                retention = RetentionTier.PERSISTENT_FACT
            )
        ),
        networkUse = NetworkUse.None
    )

    override val entryPoints = listOf(
        ModuleEntryPoint(id = "chat_tab", label = "Chat", route = "chat")
    )

    override suspend fun onActivate(context: ModuleContext): ActivationResult =
        ActivationResult.Success

    override suspend fun onDeactivate(context: ModuleContext) = Unit
}
