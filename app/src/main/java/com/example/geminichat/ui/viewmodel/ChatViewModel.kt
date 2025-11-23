package com.example.geminichat.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminichat.data.AppDatabase
import com.example.geminichat.data.ChatMessage
import com.example.geminichat.data.GeminiRepository
import com.example.geminichat.data.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.chatDao()
    private val prefs = PreferenceManager(application)
    private val repository = GeminiRepository(prefs)

    // Chat History from DB
    val chatHistory = dao.getAllMessages()

    // For real-time streaming text visibility before saving to DB
    private val _currentStreamingResponse = MutableStateFlow("")
    val currentStreamingResponse: StateFlow<String> = _currentStreamingResponse.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        // Optimization: Clean up old messages on startup
        viewModelScope.launch {
            dao.deleteOldMessages()
        }
    }

    // Preferences State
    val isCustomKeyEnabled = prefs.isCustomKeyEnabled
    val customApiKey = prefs.customApiKey
    val modelType = prefs.modelType
    val safetyLevel = prefs.safetyLevel

    fun sendMessage(text: String) {
        viewModelScope.launch {
            // 1. Save User Message
            val userMsg = ChatMessage(text = text, isUser = true)
            dao.insertMessage(userMsg)

            _isTyping.value = true

            // 2. Get Settings
            val isCustomKey = prefs.isCustomKeyEnabled.first()
            val customKey = prefs.customApiKey.first()
            val model = prefs.modelType.first()
            val safety = prefs.safetyLevel.first()

            // 3. Call AI
            val sb = StringBuilder()

            try {
                repository.generateResponse(text, isCustomKey, customKey, model, safety)
                    .collect { chunk ->
                        sb.append(chunk)
                        // Update the streaming state for UI
                        _currentStreamingResponse.value = sb.toString()
                    }

                // Final Save to DB
                val finalAiMsg = ChatMessage(text = sb.toString(), isUser = false, modelUsed = model)
                dao.insertMessage(finalAiMsg)

                // Trigger cleanup occasionally
                dao.deleteOldMessages()

            } catch (e: Exception) {
                val errorMsg = ChatMessage(text = "Error: ${e.message}", isUser = false, isError = true)
                dao.insertMessage(errorMsg)
            } finally {
                _isTyping.value = false
                _currentStreamingResponse.value = "" // Reset streaming state
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
        }
    }

    // Settings Updaters
    fun updateSettings(isCustomKey: Boolean, key: String, model: String, safety: Int) {
        viewModelScope.launch {
            prefs.setCustomKeyEnabled(isCustomKey)
            if (key.isNotEmpty()) prefs.setCustomApiKey(key)
            prefs.setModelType(model)
            prefs.setSafetyLevel(safety)
        }
    }
}
