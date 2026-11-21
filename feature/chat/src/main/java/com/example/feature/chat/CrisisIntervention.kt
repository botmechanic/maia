package com.example.feature.chat

object CrisisIntervention {
    private val CRISIS_PATTERNS = listOf(
        // English
        "\\b(suicide|suicidal)\\b",
        "\\b(kill\\s+myself|killing\\s+myself)\\b",
        "\\b(end\\s+my\\s+life|ending\\s+my\\s+life)\\b",
        "\\b(want\\s+to\\s+die|wish\\s+i\\s+were\\s+dead)\\b",
        "\\b(take\\s+my\\s+own\\s+life|taking\\s+my\\s+own\\s+life)\\b",
        "\\b(harm\\s+myself|harming\\s+myself)\\b",
        "\\b(hurt\\s+myself|hurting\\s+myself)\\b",
        "\\b(better\\s+off\\s+dead)\\b",
        "\\b(cut\\s+myself|cutting\\s+myself)\\b",
        // French
        "\\b(me\\s+suicider|me\\s+tuer|envie\\s+de\\s+mourir|en\\s+finir\\s+avec\\s+la\\s+vie)\\b",
        // Spanish
        "\\b(suicidarme|suicidio|quitarme\\s+la\\s+vida|quiero\\s+morir|hacerme\\s+daño)\\b",
        // Portuguese
        "\\b(me\\s+matar|suicídio|tirar\\s+a\\s+própria\\s+vida|quero\\s+morrer)\\b",
        // German
        "\\b(mich\\s+umbringen|selbstmord|suizid|will\\s+sterben|mir\\s+das\\s+leben\\s+nehmen)\\b",
        // Japanese
        "(死にたい|自殺したい|自分を傷つけたい|消えてしまいたい)"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    fun isCrisisDisclosure(text: String): Boolean {
        if (text.isBlank()) return false
        return CRISIS_PATTERNS.any { it.containsMatchIn(text) }
    }

    const val CRISIS_RESPONSE =
        "I hear how much pain you're in, and I care deeply about your safety. You don't have to carry this alone. Please reach out to someone who can support you right now:\n\n" +
        "• 988 Suicide & Crisis Lifeline: Call or text 988 (Available 24/7, free & confidential in the US/Canada)\n" +
        "• Crisis Text Line: Text HOME to 741741\n" +
        "• The Trevor Project (LGBTQ youth): Call 1-866-488-7386 or text START to 678-678\n" +
        "• International resources: https://findahelpline.com or https://www.befrienders.org\n\n" +
        "Please speak with someone who can be with you right now. You matter, and there is help."
}

