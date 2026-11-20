package com.example.core.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModelOutputHygieneTest {

    @Test
    fun stripsLeadingSpeakerLabels() {
        assertEquals(
            "March 12. I have it saved.",
            ModelOutputHygiene.sanitize("Maia: March 12. I have it saved.")
        )
        assertEquals(
            "That stings.",
            ModelOutputHygiene.sanitize("You said: That stings.")
        )
        assertEquals(
            "Hello.",
            ModelOutputHygiene.sanitize("User: Hello.")
        )
    }

    @Test
    fun truncatesAtUserTurnOrEndOfTurn() {
        assertEquals(
            "That stings.",
            ModelOutputHygiene.sanitize("That stings.\nUser: what next?")
        )
        assertEquals(
            "Short answer.",
            ModelOutputHygiene.sanitize("Short answer.<end_of_turn>\n<start_of_turn>user")
        )
        assertEquals(
            "September nineteenth!",
            ModelOutputHygiene.sanitize("<end_of_turn>\nMaia: September nineteenth!<end_of_turn>")
        )
    }

    @Test
    fun leavesCleanReplyAlone() {
        val reply = "That stings. Was it the material or the nerves?"
        assertEquals(reply, ModelOutputHygiene.sanitize(reply))
        assertFalse(ModelOutputHygiene.sanitize(reply).startsWith("Maia"))
    }

    @Test
    fun detectsNearDuplicates() {
        val past = listOf(
            "You're right! September nineteenth! That's correct!",
            "You're right! September nineteenth! Let's move on!"
        )
        val candidateMatch = "You're right! September nineteenth! And I've saved that as well."
        val candidateDifferent = "I can answer that for you."

        org.junit.Assert.assertTrue(ModelOutputHygiene.isNearDuplicate(candidateMatch, past))
        assertFalse(ModelOutputHygiene.isNearDuplicate(candidateDifferent, past))
    }
}
