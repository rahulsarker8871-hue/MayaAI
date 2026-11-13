package com.maya.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maya.ai.ai.AiBrain
import com.maya.ai.emotional.EmotionalEngine
import com.maya.ai.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class MayaViewModel(application: Application) : AndroidViewModel(application) {
    private val aiBrain = AiBrain(application)
    private val voiceManager = VoiceManager(application)
    private val emotionalEngine = EmotionalEngine()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("হ্যালো! আমি মায়া। আজ তুমি কেমন আছো?", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _currentMood = MutableStateFlow(emotionalEngine.getCurrentMood())
    val currentMood: StateFlow<String> = _currentMood

    fun setApiKey(key: String) = aiBrain.saveApiKey(key)
    fun getApiKey() = aiBrain.getApiKey()

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        _messages.value = _messages.value + ChatMessage(text, true)
        _isLoading.value = true

        emotionalEngine.analyzeUserMessage(text)
        _currentMood.value = emotionalEngine.getCurrentMood()

        viewModelScope.launch {
            val reply = aiBrain.generateReply(text, emotionalEngine.getEmotionalPrompt())
            _messages.value = _messages.value + ChatMessage(reply, false)
            _isLoading.value = false
            
            // Edge TTS দিয়ে ভয়েস প্লে করা (কোনো API Key লাগে না)
            voiceManager.speakWithEdgeTts(reply)
        }
    }

    fun startListening() {
        _isListening.value = true
        voiceManager.startListening(
            onResult = { text ->
                _isListening.value = false
                sendMessage(text)
            },
            onError = { error ->
                _isListening.value = false
                _messages.value = _messages.value + ChatMessage(error, false)
            }
        )
    }

    fun stopListening() {
        _isListening.value = false
        voiceManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
