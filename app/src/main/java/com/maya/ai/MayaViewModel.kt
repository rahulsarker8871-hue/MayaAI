package com.maya.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maya.ai.ai.AiBrain
import com.maya.ai.emotional.EmotionalEngine
import com.maya.ai.memory.ChatEntity
import com.maya.ai.memory.MayaDatabase
import com.maya.ai.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class MayaViewModel(application: Application) : AndroidViewModel(application) {
    private val aiBrain = AiBrain(application)
    private val voiceManager = VoiceManager(application)
    private val emotionalEngine = EmotionalEngine()
    private val db = MayaDatabase.getDatabase(application)
    private val dao = db.chatDao()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    private val _currentMood = MutableStateFlow(emotionalEngine.getCurrentMood())
    val currentMood: StateFlow<String> = _currentMood

    init {
        // ডেটাবেস থেকে পুরনো চ্যাট লোড করা
        viewModelScope.launch {
            dao.getAllMessages().collect { entities ->
                _messages.value = entities.map { ChatMessage(it.text, it.isUser, it.timestamp) }
            }
        }
    }

    fun setApiKey(key: String) = aiBrain.saveApiKey(key)
    fun getApiKey() = aiBrain.getApiKey()

    fun setTtsApiKey(key: String) {
        getApplication<Application>().getSharedPreferences("maya_prefs", Application.MODE_PRIVATE)
            .edit().putString("sarvam_key", key).apply()
    }
    fun getTtsApiKey(): String {
        return getApplication<Application>().getSharedPreferences("maya_prefs", Application.MODE_PRIVATE)
            .getString("sarvam_key", "") ?: ""
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        
        val userMsg = ChatEntity(text = text, isUser = true)
        
        viewModelScope.launch {
            dao.insertMessage(userMsg)
            _isLoading.value = true

            emotionalEngine.analyzeUserMessage(text)
            val mood = emotionalEngine.getCurrentMood()
            _currentMood.value = mood

            val emotion = when {
                mood.contains("রাগ") || mood.contains("ঈর্ষা") -> "angry"
                mood.contains("দুঃখ") -> "sad"
                mood.contains("ভালোবাসা") || mood.contains("খুশি") -> "happy"
                else -> "neutral"
            }

            // এআই-কে আগের চ্যাটগুলো পাঠানো (Context)
            val history = _messages.value.takeLast(10).map { 
                if (it.isUser) "User: ${it.text}" else "Maya: ${it.text}" 
            }.joinToString("\n")

            val reply = aiBrain.generateReply(text, history, emotionalEngine.getEmotionalPrompt())
            
            val mayaMsg = ChatEntity(text = reply, isUser = false)
            dao.insertMessage(mayaMsg)
            
            _isLoading.value = false
            
            val ttsKey = getTtsApiKey()
            if (ttsKey.isNotEmpty()) {
                voiceManager.speakWithSarvam(reply, ttsKey, emotion)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearHistory()
        }
    }

    fun startListening() {
        _isListening.value = true
        voiceManager.startListening(
            onResult = { text -> _isListening.value = false; sendMessage(text) },
            onError = { error -> _isListening.value = false }
        )
    }

    fun stopListening() { _isListening.value = false; voiceManager.stopListening() }
    override fun onCleared() { super.onCleared(); voiceManager.destroy() }
}
