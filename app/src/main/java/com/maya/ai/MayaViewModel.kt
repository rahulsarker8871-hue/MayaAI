package com.maya.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maya.ai.ai.AiBrain
import com.maya.ai.emotional.EmotionalEngine
import com.maya.ai.memory.ChatEntity
import com.maya.ai.memory.MayaDatabase
import com.maya.ai.memory.MemoryEntity
import com.maya.ai.phone.PhoneController
import com.maya.ai.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class MayaViewModel(application: Application) : AndroidViewModel(application) {
    private val aiBrain = AiBrain(application)
    private val voiceManager = VoiceManager(application)
    private val emotionalEngine = EmotionalEngine()
    private val phoneController = PhoneController(application)
    private val db = MayaDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val memoryDao = db.memoryDao()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    private val _currentMood = MutableStateFlow(emotionalEngine.getCurrentMood())
    val currentMood: StateFlow<String> = _currentMood

    private var longTermMemoryString = ""

    init {
        viewModelScope.launch { chatDao.getAllMessages().collect { entities -> _messages.value = entities.map { ChatMessage(it.text, it.isUser, it.timestamp) } } }
        viewModelScope.launch { memoryDao.getAllMemories().collect { memories -> longTermMemoryString = memories.joinToString("\n") { "- ${it.fact}" } } }
    }

    fun setApiKey(key: String) = aiBrain.saveApiKey(key)
    fun getApiKey() = aiBrain.getApiKey()
    fun setTtsApiKey(key: String) { getApplication<Application>().getSharedPreferences("maya_prefs", Application.MODE_PRIVATE).edit().putString("sarvam_key", key).apply() }
    fun getTtsApiKey(): String { return getApplication<Application>().getSharedPreferences("maya_prefs", Application.MODE_PRIVATE).getString("sarvam_key", "") ?: "" }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        viewModelScope.launch {
            chatDao.insertMessage(ChatEntity(text = text, isUser = true))
            _isLoading.value = true

            emotionalEngine.analyzeUserMessage(text)
            val mood = emotionalEngine.getCurrentMood()
            _currentMood.value = mood
            val emotion = when { mood.contains("রাগ") || mood.contains("ঈর্ষা") -> "angry"; mood.contains("দুঃখ") -> "sad"; mood.contains("ভালোবাসা") || mood.contains("খুশি") -> "happy"; else -> "neutral" }

            val history = _messages.value.takeLast(10).map { if (it.isUser) "User: ${it.text}" else "Maya: ${it.text}" }.joinToString("\n")
            var rawReply = aiBrain.generateReply(text, history, longTermMemoryString, emotionalEngine.getEmotionalPrompt())
            
            val factRegex = Regex("\\[FACT:\\s*(.*?)\\]")
            val factMatch = factRegex.find(rawReply)
            if (factMatch != null) {
                memoryDao.insertMemory(MemoryEntity(fact = factMatch.groupValues[1].replace("_", " ")))
                rawReply = rawReply.replace(factMatch.value, "").trim()
            }

            var actionResult = ""
            val actionRegex = Regex("\\[ACTION:\\s*(.*?)\\]")
            val actionMatch = actionRegex.find(rawReply)
            if (actionMatch != null) {
                val parts = actionMatch.groupValues[1].split(":")
                actionResult = when (parts[0]) {
                    "OPEN_APP" -> if (parts.size > 1) phoneController.openApp(parts[1]) else "অ্যাপের নাম বলোনি।"
                    "YOUTUBE_SEARCH" -> if (parts.size > 1) phoneController.searchYouTube(parts[1]) else "কী সার্চ করতে হবে বলোনি।"
                    "WHATSAPP_MSG" -> if (parts.size > 2) phoneController.sendWhatsAppMessage(parts[1], parts[2]) else "নাম্বার বা মেসেজ দাওনি।"
                    "MEDIA_PLAY_PAUSE" -> phoneController.togglePlayPause()
                    "MEDIA_NEXT" -> phoneController.nextTrack()
                    "SET_ALARM" -> if (parts.size > 1) {
                        val timeParts = parts[1].split(":")
                        if (timeParts.size == 2) phoneController.setAlarm(timeParts[0].toIntOrNull() ?: 8, timeParts[1].toIntOrNull() ?: 0) else "সময় বুঝতে পারিনি।"
                    } else "কখন অ্যালার্ম দেব?"
                    "GET_BATTERY" -> phoneController.getBatteryLevel()
                    "CALL" -> if (parts.size > 1) phoneController.makeCall(parts[1]) else "নাম্বার দাওনি।"
                    else -> ""
                }
                rawReply = rawReply.replace(actionMatch.value, "").trim()
            }

            val finalReply = if (actionResult.isNotEmpty()) "$rawReply\n\n($actionResult)" else rawReply
            chatDao.insertMessage(ChatEntity(text = finalReply, isUser = false))
            _isLoading.value = false
            
            val ttsKey = getTtsApiKey()
            if (ttsKey.isNotEmpty()) voiceManager.speakWithSarvam(finalReply, ttsKey, emotion)
        }
    }

    fun clearHistory() { viewModelScope.launch { chatDao.clearHistory() } }
    fun startListening() { _isListening.value = true; voiceManager.startListening(onResult = { text -> _isListening.value = false; sendMessage(text) }, onError = { _isListening.value = false }) }
    fun stopListening() { _isListening.value = false; voiceManager.stopListening() }
    override fun onCleared() { super.onCleared(); voiceManager.destroy() }
}
