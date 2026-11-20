package com.example.core.contract

import com.example.core.model.ChatGenerationRequest
import com.example.core.model.ChatRole
import com.example.core.model.ChatTurn

/**
 * Builds Gemma-it native multi-turn prompts without transcript/screenplay labels.
 * History is injected as real &lt;start_of_turn&gt; user/model turns.
 */
object GemmaChatFormatting {

    data class PromptChunk(
        val role: String,
        val preview: String
    )

    fun formatRetrievedMoment(score: Float, timeLabel: String, content: String): String =
        "- ($timeLabel, similarity ${"%.2f".format(score)}) $content"

    /**
     * Chunk sequence for logging/verification, then the exact string passed to the model.
     * Opening user turn = system preamble + first user text (history head or new message).
     * Remaining history alternates model/user; ends ready for model completion.
     */
    fun buildChunkSequence(request: ChatGenerationRequest): List<PromptChunk> {
        val chunks = mutableListOf<PromptChunk>()
        val history = request.history
        val firstUser = history.firstOrNull { it.role == ChatRole.USER }?.text
            ?: request.userMessage

        val opening = buildString {
            append(request.systemPreamble.trimEnd())
            append("\n\n")
            append(firstUser)
        }
        chunks += PromptChunk("user", opening.take(40))

        var i = 0
        // Skip the first USER turn (already in opening)
        while (i < history.size && history[i].role != ChatRole.USER) i++
        if (i < history.size && history[i].role == ChatRole.USER) i++

        while (i < history.size) {
            val turn = history[i]
            when (turn.role) {
                ChatRole.MODEL -> chunks += PromptChunk("model", turn.text.take(40))
                ChatRole.USER -> chunks += PromptChunk("user", turn.text.take(40))
            }
            i++
        }

        // If history was empty, opening already has the new user message.
        // If history was non-empty, append the new user message as a final user turn
        // unless it is already the opening (history empty case).
        val historyHadUser = history.any { it.role == ChatRole.USER }
        if (historyHadUser) {
            chunks += PromptChunk("user", request.userMessage.take(40))
        }
        chunks += PromptChunk("model", "(generate)")
        return chunks
    }

    fun buildGemmaPrompt(request: ChatGenerationRequest): String {
        val history = request.history
        val firstUser = history.firstOrNull { it.role == ChatRole.USER }?.text
            ?: request.userMessage

        return buildString {
            append("<start_of_turn>user\n")
            append(request.systemPreamble.trimEnd())
            append("\n\n")
            append(firstUser)
            append("\n<end_of_turn>\n")

            var i = 0
            while (i < history.size && history[i].role != ChatRole.USER) i++
            if (i < history.size && history[i].role == ChatRole.USER) i++

            while (i < history.size) {
                val turn = history[i]
                when (turn.role) {
                    ChatRole.MODEL -> {
                        append("<start_of_turn>model\n")
                        append(turn.text.trim())
                        append("\n<end_of_turn>\n")
                    }
                    ChatRole.USER -> {
                        append("<start_of_turn>user\n")
                        append(turn.text.trim())
                        append("\n<end_of_turn>\n")
                    }
                }
                i++
            }

            val historyHadUser = history.any { it.role == ChatRole.USER }
            if (historyHadUser) {
                append("<start_of_turn>user\n")
                append(request.userMessage.trim())
                append("\n<end_of_turn>\n")
            }
            append("<start_of_turn>model\n")
        }
    }
}
