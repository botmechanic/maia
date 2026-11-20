package com.example.core.contract

import com.example.core.model.ChatGenerationRequest
import com.example.core.model.ChatRole
import com.example.core.model.ChatTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GemmaChatFormattingTest {

    @Test
    fun chunkSequenceForThreeTurnSession() {
        val request = ChatGenerationRequest(
            systemPreamble = "You are Maia.\n\nYOUR MEMORY OF THE USER:\nBirthday: March 12",
            history = listOf(
                ChatTurn(ChatRole.USER, "hi"),
                ChatTurn(ChatRole.MODEL, "Hello. I'm here."),
                ChatTurn(ChatRole.USER, "when is my birthday?"),
                ChatTurn(ChatRole.MODEL, "March 12. I have it saved.")
            ),
            userMessage = "what else do you know?"
        )
        val chunks = GemmaChatFormatting.buildChunkSequence(request)
        println("==== CHUNK SEQUENCE ====")
        chunks.forEachIndexed { i, c -> println("[$i] ${c.role}: ${c.preview}") }
        println("==== FULL PROMPT (first 500) ====")
        val prompt = GemmaChatFormatting.buildGemmaPrompt(request)
        println(prompt.take(500))
        println("...")

        assertEquals("user", chunks.first().role)
        assertEquals("model", chunks.last().role)
        assertTrue(chunks.any { it.role == "model" && it.preview.startsWith("Hello") })
        assertTrue(prompt.contains("<start_of_turn>user\n"))
        assertTrue(prompt.contains("<start_of_turn>model\n"))
        assertTrue(prompt.endsWith("<start_of_turn>model\n"))
        assertFalse(prompt.contains("they said:"))
        assertFalse(prompt.contains("You said:"))
        assertFalse(prompt.contains("RECENT CONVERSATION"))
    }

    @Test
    fun retrievedMomentHasNoSpeakerLabels() {
        val line = GemmaChatFormatting.formatRetrievedMoment(0.81f, "2026-07-20 10:00", "My birthday is March 12")
        assertFalse(line.contains("they said"))
        assertFalse(line.contains("USER:"))
        assertTrue(line.contains("My birthday is March 12"))
    }

    @Test
    fun templateHasNoConversationScaffold() {
        val asset = listOf(
            File("../character/src/main/assets/maia_system_prompt.txt"),
            File("core/character/src/main/assets/maia_system_prompt.txt"),
            File("src/main/assets/maia_system_prompt.txt")
        ).firstOrNull { it.exists() } ?: return
        val text = asset.readText()
        assertFalse(text.contains("RECENT CONVERSATION"))
        assertFalse(text.contains("{{CONVERSATION_HISTORY}}"))
        assertFalse(text.contains("You say:"))
        assertTrue(text.contains("respond like:"))
    }
}
