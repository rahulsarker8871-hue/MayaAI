package com.maya.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maya.ai.ChatMessage
import com.maya.ai.MayaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MayaChatScreen(viewModel: MayaViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(viewModel.getApiKey().isEmpty()) }
    var apiKeyInput by remember { mutableStateOf(viewModel.getApiKey()) }
    var sarvamKeyInput by remember { mutableStateOf(viewModel.getTtsApiKey()) }

    // পাসওয়ার্ড এবং আনলক স্টেট
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) viewModel.startListening() else Toast.makeText(context, "মাইক পারমিশন দরকার!", Toast.LENGTH_SHORT).show()
        }
    )

    Scaffold(
        containerColor = Color(0xFF0D0D1A),
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // AnimatedContent এর মাধ্যমে "N" থেকে "NURI X RAHUL" এ পরিবর্তন হবে
                        AnimatedContent(
                            targetState = isUnlocked,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.5f, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)))
                                    .togetherWith(scaleOut(targetScale = 0.5f, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)))
                            }, label = "TitleAnimation"
                        ) { unlocked ->
                            if (!unlocked) {
                                Text(
                                    text = "N",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE91E63),
                                    modifier = Modifier.clickable { showPasswordDialog = true }
                                )
                            } else {
                                Text(
                                    text = "NURI X RAHUL",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE91E63)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFE91E63))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D0D1A))
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
                
                IconButton(onClick = { 
                    if (isListening) viewModel.stopListening()
                    else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.startListening()
                        else recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic", tint = if (isListening) Color.Red else Color(0xFFE91E63))
                }
                
                IconButton(onClick = { viewModel.sendMessage(input); input = "" }, enabled = !isLoading && !isListening) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFE91E63))
                }
            }
        }
    }

    // পাসওয়ার্ড ডায়ালগ
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; passwordInput = "" },
            title = { Text("Password Day") },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Enter Password") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (passwordInput == "rahul") { // এখানে পাসওয়ার্ড 
                        showPasswordDialog = false
                        isUnlocked = true // পাসওয়ার্ড সঠিক হলে NURI X RAHUL হয়ে যাবে
                        passwordInput = ""
                    } else {
                        Toast.makeText(context, "ভুল পাসওয়ার্ড!", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Unlock") }
            }
        )
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
                    Button(onClick = { viewModel.clearHistory(); showSettings = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Clear Chat History", color = Color.White)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setApiKey(apiKeyInput); viewModel.setTtsApiKey(sarvamKeyInput); showSettings = false }) { Text("Save") }
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
