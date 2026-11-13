package com.maya.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maya.ai.ChatMessage
import com.maya.ai.MayaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MayaChatScreen(viewModel: MayaViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val currentMood by viewModel.currentMood.collectAsStateWithLifecycle()
    
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(viewModel.getApiKey().isEmpty()) }
    var apiKeyInput by remember { mutableStateOf(viewModel.getApiKey()) }
    var sarvamKeyInput by remember { mutableStateOf(viewModel.getTtsApiKey()) }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("MAYA AI", fontWeight = FontWeight.Bold)
                        Text("Mood: $currentMood", fontSize = 12.sp, color = Color.LightGray)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFE91E63))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D1A), titleContentColor = Color(0xFFE91E63))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("মায়ার সাথে কথা বলা শুরু করো... ❤️", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp)) {
                    items(messages) { msg -> ChatBubble(msg) }
                }
            }

            if (isLoading) { Text("মায়া ভাবছে...", color = Color.Gray, modifier = Modifier.padding(8.dp), fontSize = 12.sp) }
            if (isListening) { Text("মায়া শুনছে... 🎙️", color = Color(0xFFE91E63), modifier = Modifier.padding(8.dp), fontSize = 12.sp) }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("মায়ার সাথে কথা বলো...", color = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFFE91E63), unfocusedBorderColor = Color.Gray)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { if (isListening) viewModel.stopListening() else viewModel.startListening() }) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic", tint = if (isListening) Color.Red else Color(0xFFE91E63))
                }
                IconButton(onClick = { viewModel.sendMessage(input); input = "" }, enabled = !isLoading && !isListening) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFE91E63))
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { if(viewModel.getApiKey().isNotEmpty()) showSettings = false },
            title = { Text("API Settings") },
            text = {
                Column {
                    OutlinedTextField(value = apiKeyInput, onValueChange = { apiKeyInput = it }, label = { Text("OpenRouter Key") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    OutlinedTextField(value = sarvamKeyInput, onValueChange = { sarvamKeyInput = it }, label = { Text("Sarvam AI Key") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        viewModel.clearHistory() 
                        showSettings = false 
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Clear Chat History", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.setApiKey(apiKeyInput)
                    viewModel.setTtsApiKey(sarvamKeyInput)
                    showSettings = false 
                }) { Text("Save") }
            }
        )
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) Color(0xFFE91E63) else Color(0xFF2A2A4A)
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Text(text = msg.text, color = Color.White, modifier = Modifier.background(bgColor, RoundedCornerShape(16.dp)).padding(12.dp).widthIn(max = 280.dp))
    }
}
