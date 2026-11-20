package com.example.feature.chat

import android.content.SharedPreferences
import java.util.UUID

/**
 * Conversation page id. Semantic memory + distillation still span all sessions;
 * only the chat UI history window and native multi-turn prompt use the current id.
 */
class ConversationSessionStore(
    private val prefs: SharedPreferences
) {
    @Volatile
    private var currentId: String =
        prefs.getString(KEY_CURRENT_SESSION, null) ?: newSessionInternal()

    val currentSessionId: String
        get() = currentId

    /** Cold start / New conversation — fresh page; memories stay. */
    fun startNewSession(): String = newSessionInternal()

    private fun newSessionInternal(): String {
        val id = UUID.randomUUID().toString()
        currentId = id
        prefs.edit().putString(KEY_CURRENT_SESSION, id).apply()
        return id
    }

    companion object {
        private const val KEY_CURRENT_SESSION = "chat_session_id"
    }
}
