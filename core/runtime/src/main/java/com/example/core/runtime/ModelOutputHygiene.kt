package com.example.core.runtime

/**
 * Post-process Gemma-it generations. MediaPipe 0.10.29 has no stop-sequence API.
 */
object ModelOutputHygiene {

    private val leadingSpeakerLabel = Regex(
        """^(?i)(maia|you said|user|model|assistant)\s*:\s*"""
    )

    private val leadingTurnMarker = Regex(
        """^(?i)(\s*<end_of_turn>|\s*<start_of_turn>(model|user)?)+"""
    )

    fun sanitize(raw: String): String {
        var text = raw.trimStart()

        // Strip leading turn markers first
        while (leadingTurnMarker.containsMatchIn(text)) {
            text = leadingTurnMarker.replaceFirst(text, "").trimStart()
        }

        // Strip leading speaker-label patterns the model may parrot
        while (leadingSpeakerLabel.containsMatchIn(text)) {
            text = leadingSpeakerLabel.replaceFirst(text, "").trimStart()
        }

        val cutAt = listOf(
            text.indexOf("\nUser:"),
            text.indexOf("\nuser:"),
            text.indexOf("\nMaia:"),
            text.indexOf("\nYou said:"),
            text.indexOf("<end_of_turn>"),
            text.indexOf("\n<start_of_turn>")
        ).filter { it >= 0 }.minOrNull()

        if (cutAt != null) {
            text = text.substring(0, cutAt)
        }
        return text.trimEnd()
    }

    fun normalizePrefix(text: String, len: Int = 25): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .take(len)
    }

    fun isNearDuplicate(candidate: String, previousReplies: List<String>): Boolean {
        val normCandidate = normalizePrefix(candidate)
        if (normCandidate.isEmpty()) return false
        return previousReplies.any { prev ->
            val normPrev = normalizePrefix(prev)
            normPrev.isNotEmpty() && (normCandidate == normPrev || normCandidate.startsWith(normPrev) || normPrev.startsWith(normCandidate))
        }
    }
}
