package com.example.feature.settings

import com.example.core.contract.ActivationResult
import com.example.core.contract.LadderPosition
import com.example.core.contract.MaiaModule
import com.example.core.contract.ModuleContext
import com.example.core.contract.ModuleEntryPoint
import com.example.core.contract.ModuleManifest
import com.example.core.contract.NetworkUse

class SettingsModule : MaiaModule {
    override val manifest = ModuleManifest(
        id = "settings",
        displayName = "Settings",
        maiaDescription = "I'll let you choose how I look and which on-device model I use, " +
            "and I can wipe what I've remembered if you ask. I don't need phone permissions for that — " +
            "everything stays between us on this device.",
        ladderCeiling = LadderPosition.SUGGEST,
        androidPermissions = emptyList(),
        specialAccess = emptyList(),
        observes = emptyList(),
        writes = emptyList(),
        networkUse = NetworkUse.None
    )

    override val entryPoints = listOf(
        ModuleEntryPoint(id = "settings_tab", label = "Settings", route = "settings")
    )

    override suspend fun onActivate(context: ModuleContext): ActivationResult =
        ActivationResult.Success

    override suspend fun onDeactivate(context: ModuleContext) = Unit
}
