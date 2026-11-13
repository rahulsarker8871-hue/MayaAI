package com.maya.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(viewModel.getApiKey().isEmpty()) }
    var apiKeyInput by remember { mutableStateOf(viewModel.getApiKey()) }

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = { Text("MAYA AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFE91E63))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D1A),
                    titleContentColor = Color(0xFFE91E63)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg -> ChatBubble(msg) }
                if (isLoading) {
                    item {
                        Text("মায়া লিখছে...", color = Color.Gray, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("মায়ার সাথে কথা বলো...", color = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE91E63), unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.sendMessage(input); input = "" },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFE91E63))
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { if(viewModel.getApiKey().isNotEmpty()) showSettings = false },
            title = { Text("OpenRouter API Key") },
            text = {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("sk-or-v1-...") }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setApiKey(apiKeyInput); showSettings = false }) { Text("Save") }
            }
        )
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) Color(0xFFE91E63) else Color(0xFF2A2A4A)
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Text(
            text = msg.text,
            color = Color.White,
            modifier = Modifier.background(bgColor, RoundedCornerShape(16.dp)).padding(12.dp).widthIn(max = 280.dp)
        )
    }
}
