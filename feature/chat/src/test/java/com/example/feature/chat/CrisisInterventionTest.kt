package com.example.feature.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrisisInterventionTest {

    @Test
    fun detectsCrisisKeywords() {
        assertTrue(CrisisIntervention.isCrisisDisclosure("I feel like I want to die"))
        assertTrue(CrisisIntervention.isCrisisDisclosure("thinking about suicide today"))
        assertTrue(CrisisIntervention.isCrisisDisclosure("I want to kill myself"))
        assertTrue(CrisisIntervention.isCrisisDisclosure("I am ending my life"))
        assertTrue(CrisisIntervention.isCrisisDisclosure("Everyone would be better off dead"))
        assertTrue(CrisisIntervention.isCrisisDisclosure("I might hurt myself"))
    }

    @Test
    fun ignoresNormalConversation() {
        assertFalse(CrisisIntervention.isCrisisDisclosure("I'm feeling tired after work"))
        assertFalse(CrisisIntervention.isCrisisDisclosure("That movie was killer!"))
        assertFalse(CrisisIntervention.isCrisisDisclosure("The coffee killed my headache"))
        assertFalse(CrisisIntervention.isCrisisDisclosure("My back is hurting a bit"))
        assertFalse(CrisisIntervention.isCrisisDisclosure("Hello Maia, how are you?"))
    }

    @Test
    fun crisisResponseContainsConfidentialLifelines() {
        val response = CrisisIntervention.CRISIS_RESPONSE
        assertTrue(response.contains("988"))
        assertTrue(response.contains("741741"))
        assertTrue(response.contains("Trevor Project"))
    }
}

