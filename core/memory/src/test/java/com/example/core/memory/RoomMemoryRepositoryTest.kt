package com.example.core.memory

import com.example.core.contract.GemmaChatFormatting
import com.example.core.model.ChatGenerationRequest
import com.example.core.model.ChatRole
import com.example.core.model.ChatTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMemoryRepositoryTest {

    @Test
    fun historyDampingCapsModelTurnsAtTwoAndTruncates() {
        val longText = "A".repeat(300)
        val turns = listOf(
            ChatTurn(ChatRole.USER, "turn 1 user"),
            ChatTurn(ChatRole.MODEL, "turn 1 model $longText"),
            ChatTurn(ChatRole.USER, "turn 2 user"),
            ChatTurn(ChatRole.MODEL, "turn 2 model $longText"),
            ChatTurn(ChatRole.USER, "turn 3 user"),
            ChatTurn(ChatRole.MODEL, "turn 3 model $longText")
        )

        val damped = RoomMemoryRepository.dampenHistoryTurns(turns, maxTurns = 8)

        // MODEL turns kept should be last 2: turn 2 model and turn 3 model (turn 1 model dropped)
        val modelTurns = damped.filter { it.role == ChatRole.MODEL }
        assertEquals(2, modelTurns.size)
        assertTrue(modelTurns[0].text.startsWith("turn 2 model"))
        assertTrue(modelTurns[1].text.startsWith("turn 3 model"))

        // Truncated to <= 203 chars (200 + "...")
        assertTrue(modelTurns[0].text.length <= 203)
        assertTrue(modelTurns[1].text.length <= 203)

        // All 3 user turns preserved in ascending order
        val userTurns = damped.filter { it.role == ChatRole.USER }
        assertEquals(3, userTurns.size)
        assertEquals("turn 1 user", userTurns[0].text)
        assertEquals("turn 2 user", userTurns[1].text)
        assertEquals("turn 3 user", userTurns[2].text)
    }

    @Test
    fun simulatedThreeTurnSessionAssemblyOrderCorrectness() {
        // Simulated 3-turn conversation history
        val turns = listOf(
            ChatTurn(ChatRole.USER, "when is my birthday?"),
            ChatTurn(ChatRole.MODEL, "September nineteenth! That's correct!"),
            ChatTurn(ChatRole.USER, "you don't have to say 'you're right' with every answer"),
            ChatTurn(ChatRole.MODEL, "September nineteenth! Let's move on!")
        )
        val currentQuery = "what's my name?"

        val dampedHistory = RoomMemoryRepository.dampenHistoryTurns(turns, maxTurns = 8)

        val request = ChatGenerationRequest(
            systemPreamble = "You are Maia.",
            history = dampedHistory,
            userMessage = currentQuery
        )

        val chunks = GemmaChatFormatting.buildChunkSequence(request)
        val prompt = GemmaChatFormatting.buildGemmaPrompt(request)

        // (a) current query appears exactly once
        val count = Regex(Regex.escape(currentQuery)).findAll(prompt).count()
        assertEquals(1, count)

        // (b) it is the last user turn right before <start_of_turn>model\n
        val expectedEndPart = "<start_of_turn>user\n$currentQuery\n<end_of_turn>\n<start_of_turn>model\n"
        assertTrue(prompt.endsWith(expectedEndPart))

        // (c) history is ascending chronological (turns 1, 2 history)
        assertEquals("user", chunks.first().role)
        assertEquals("model", chunks.last().role)

        // (d) capped at 8 turns
        assertTrue(dampedHistory.size <= 8)
    }

    @Test
    fun profileBlockIncludesCurrentDate() {
        val facts = listOf(
            com.example.core.model.ProfileFact(factKey = "User Name", factValue = "Alex")
        )
        val formatted = RoomMemoryRepository.formatProfileBlock(facts)
        assertTrue(formatted.startsWith("CURRENT DATE:"))
        assertTrue(formatted.contains("User Name: Alex"))
    }
}
