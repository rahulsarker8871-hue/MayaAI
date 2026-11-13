package com.maya.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maya.ai.ai.AiBrain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class MayaViewModel(application: Application) : AndroidViewModel(application) {
    private val aiBrain = AiBrain(application)
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("হ্যালো! আমি মায়া। তোমার সাথে কথা বলতে পারলে খুব ভালো লাগবে। ❤️", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun setApiKey(key: String) = aiBrain.saveApiKey(key)
    fun getApiKey() = aiBrain.getApiKey()

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return
        _messages.value = _messages.value + ChatMessage(text, true)
        _isLoading.value = true

        viewModelScope.launch {
            val reply = aiBrain.generateReply(text)
            _messages.value = _messages.value + ChatMessage(reply, false)
            _isLoading.value = false
        }
    }
}
