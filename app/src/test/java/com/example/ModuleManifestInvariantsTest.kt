package com.example

import com.example.core.contract.NetworkUse
import com.example.core.contract.ModuleRegistry
import com.example.feature.chat.ChatModule
import com.example.feature.memoryview.MemoryViewModule
import com.example.feature.settings.SettingsModule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleManifestInvariantsTest {

    private val registry = ModuleRegistry(
        listOf(ChatModule(), MemoryViewModule(), SettingsModule())
    )

    @Test
    fun everyModuleHasStableIdAndMaiaVoiceDescription() {
        registry.all().forEach { module ->
            val m = module.manifest
            assertTrue("id must be non-blank for ${m.displayName}", m.id.isNotBlank())
            assertTrue(
                "maiaDescription must be present for ${m.id}",
                m.maiaDescription.isNotBlank()
            )
        }
    }

    @Test
    fun v1ModulesDeclareNoNetwork() {
        registry.all().forEach { module ->
            assertTrue(
                "${module.manifest.id} must declare NetworkUse.None in v1",
                module.manifest.networkUse is NetworkUse.None
            )
        }
    }

    @Test
    fun v1ModulesDeclareNoAndroidPermissions() {
        registry.all().forEach { module ->
            assertTrue(
                "${module.manifest.id} should not require Android permissions in v1",
                module.manifest.androidPermissions.isEmpty()
            )
        }
    }

    @Test
    fun registryContainsExpectedCoreFeatures() {
        val ids = registry.all().map { it.manifest.id }.toSet()
        assertTrue(ids.contains("chat"))
        assertTrue(ids.contains("memoryview"))
        assertTrue(ids.contains("settings"))
        assertFalse(ids.contains(""))
    }
}
