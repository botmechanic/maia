package com.example.core.memory

import com.example.core.memory.worker.DistillationWorker
import com.example.core.model.EpisodicEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistillationWorkerTest {

    @Test
    fun extractsMultipleHeuristicFactsFromConversation() {
        val events = listOf(
            EpisodicEvent(id = 1, content = "Hi, my name is Jordan", type = "USER"),
            EpisodicEvent(id = 2, content = "Hello Jordan! How are you?", type = "MAIA"),
            EpisodicEvent(id = 3, content = "My birthday is on September 19th", type = "USER"),
            EpisodicEvent(id = 4, content = "My favorite coffee is oat milk latte", type = "USER"),
            EpisodicEvent(id = 5, content = "I work as a software engineer", type = "USER"),
            EpisodicEvent(id = 6, content = "I live in Seattle", type = "USER"),
            EpisodicEvent(id = 7, content = "I feel anxious about tomorrow's presentation", type = "USER")
        )

        val facts = DistillationWorker.extractHeuristicFacts(events)

        assertTrue(facts.any { it.key == "User Name" && it.value == "Jordan" })
        assertTrue(facts.any { it.key == "Birthday" && it.value.contains("September 19") })
        assertTrue(facts.any { it.key == "Favorite Coffee" && it.value == "oat milk latte" })
        assertTrue(facts.any { it.key == "Profession" && it.value == "software engineer" })
        assertTrue(facts.any { it.key == "Location" && it.value == "Seattle" })
        assertTrue(facts.any { it.key == "Recent Mood" && it.value.contains("Anxious") })
    }

    @Test
    fun ignoresMaiaMessagesForHeuristicExtraction() {
        val events = listOf(
            EpisodicEvent(id = 1, content = "My name is Maia and I live on this phone", type = "MAIA")
        )
        val facts = DistillationWorker.extractHeuristicFacts(events)
        assertTrue(facts.isEmpty())
    }
}

