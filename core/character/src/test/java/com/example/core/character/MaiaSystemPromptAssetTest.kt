package com.example.core.character

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MaiaSystemPromptAssetTest {

    private fun loadTemplate(): String {
        val candidates = listOf(
            File("src/main/assets/maia_system_prompt.txt"),
            File("core/character/src/main/assets/maia_system_prompt.txt")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("maia_system_prompt.txt not found from ${File(".").absolutePath}")
        return file.readText()
    }

    @Test
    fun templateHasNoTranscriptScaffold() {
        val template = loadTemplate()
        assertTrue(template.contains("{{PROFILE_FACTS}}"))
        assertTrue(template.contains("{{RETRIEVED_MEMORIES}}"))
        assertFalse(template.contains("{{CONVERSATION_HISTORY}}"))
        assertFalse(template.contains("RECENT CONVERSATION"))
        assertFalse(template.contains("{{USER_QUERY}}"))
        assertFalse(template.trimEnd().endsWith("Maia:"))
        assertFalse(template.contains("loving"))
        assertFalse(template.contains("nurturing"))
        assertTrue(template.contains("respond like:"))
    }
}
