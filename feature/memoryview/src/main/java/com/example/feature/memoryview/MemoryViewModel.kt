package com.example.feature.memoryview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.contract.MemoryRepository
import com.example.core.model.EpisodicEvent
import com.example.core.model.ProfileFact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(
    application: Application,
    val memoryRepository: MemoryRepository
) : AndroidViewModel(application) {

    val episodicMemories: StateFlow<List<EpisodicEvent>> = memoryRepository.allEpisodicMemories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeProfileMemories: StateFlow<List<ProfileFact>> =
        memoryRepository.allActiveProfileMemories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val selectedCategory = kotlinx.coroutines.flow.MutableStateFlow("All")

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        selectedCategory.value = category
    }

    fun deleteProfileFact(id: Long) {
        viewModelScope.launch { memoryRepository.deleteProfileFact(id) }
    }

    fun deleteEpisodicEvent(id: Long) {
        viewModelScope.launch { memoryRepository.deleteEpisodicMemory(id) }
    }

    fun addProfileFact(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryRepository.addProfileMemory(
                key = key.trim(),
                value = value.trim(),
                sourceEventIds = emptyList(),
                moduleId = "chat",
                source = "manual"
            )
        }
    }

    fun updateProfileFact(id: Long, key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryRepository.deleteProfileFact(id)
            memoryRepository.addProfileMemory(
                key = key.trim(),
                value = value.trim(),
                sourceEventIds = emptyList(),
                moduleId = "chat",
                source = "user_edited"
            )
        }
    }

    fun triggerImmediateDistillation() {
        memoryRepository.scheduleImmediateDistillation()
    }

    fun exportVaultMarkdown(): String {
        val facts = activeProfileMemories.value
        val episodics = episodicMemories.value
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date())

        val sb = StringBuilder()
        sb.append("# Maia Memory Vault Export\n")
        sb.append("Generated: $dateStr\n")
        sb.append("Storage: Encrypted Local SQLCipher + Android Keystore\n")
        sb.append("Privacy: 100% On-Device (Zero Network / Telemetry)\n\n")

        sb.append("## Core Profile Facts (${facts.size})\n")
        if (facts.isEmpty()) {
            sb.append("*(No active profile facts yet)*\n")
        } else {
            facts.forEach { fact ->
                sb.append("- **${fact.factKey}**: ${fact.factValue} *(source: ${fact.source})*\n")
            }
        }
        sb.append("\n")

        sb.append("## Episodic Conversation Log (${episodics.size} events)\n")
        if (episodics.isEmpty()) {
            sb.append("*(No conversation events recorded yet)*\n")
        } else {
            episodics.takeLast(50).forEach { event ->
                val time = sdf.format(java.util.Date(event.timestamp))
                sb.append("- [$time] ${event.type}: ${event.content}\n")
            }
        }

        return sb.toString()
    }
}
