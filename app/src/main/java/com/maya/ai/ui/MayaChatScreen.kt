package com.maya.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }

    // মায়ার পালস অ্যানিমেশন (কথা বলার বা শোনার সময়)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isLoading || isListening) 1.1f else 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) viewModel.startListening() else Toast.makeText(context, "মাইক পারমিশন দরকার!", Toast.LENGTH_SHORT).show()
        }
    )

    Scaffold(
        containerColor = Color.Transparent, // ব্যাকগ্রাউন্ড আমরা নিচে দিচ্ছি
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = isUnlocked,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.5f, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)))
                                    .togetherWith(scaleOut(targetScale = 0.5f, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)))
                            }, label = "TitleAnimation"
                        ) { unlocked ->
                            if (!unlocked) {
                                Text("N", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63), modifier = Modifier.clickable { showPasswordDialog = true })
                            } else {
                                Text("NURI X RAHUL", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFFE91E63))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xCC0D0D1A))
            )
        }
    ) { padding ->
        // গ্রেডিয়েন্ট ব্যাকগ্রাউন্ড
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D0D1A), Color(0xFF1A1A2E), Color(0xFF0D0D1A))
                    )
                )
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                
                // মায়ার অবতার (Avatar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        // এখানে আপনার একটি সুন্দর মেয়ের ছবি দিতে পারেন, আপাতত অ্যাপের আইকন দিচ্ছি
                        painter = painterResource(id = com.maya.ai.R.drawable.ic_launcher), 
                        contentDescription = "Maya Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale) // পালস অ্যানিমেশন
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A4A))
                    )
                }

                if (messages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("মায়ার সাথে কথা বলা শুরু করো... ❤️", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                        items(messages) { msg -> GlassChatBubble(msg) }
                    }
                }

                if (isLoading) {
                    TypingIndicator()
                }

                // ইনপুট এরিয়া (Glassmorphism)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(30.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(30.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input, 
                        onValueChange = { input = it }, 
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("মায়ার সাথে কথা বলো...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White, 
                            unfocusedTextColor = Color.White
                        )
                    )
                    
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
    }

    // পাসওয়ার্ড ডায়ালগ (আগের মতোই)
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false; passwordInput = "" },
            title = { Text("Password Day") },
            text = { OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = { Text("Enter Password") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black)) },
            confirmButton = {
                TextButton(onClick = {
                    if (passwordInput == "rahul") { showPasswordDialog = false; isUnlocked = true; passwordInput = "" } 
                    else { Toast.makeText(context, "ভুল পাসওয়ার্ড!", Toast.LENGTH_SHORT).show() }
                }) { Text("Unlock") }
            }
        )
    }

    // সেটিংস ডায়ালগ (আগের মতোই)
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { if(viewModel.getApiKey().isNotEmpty()) showSettings = false },
            title = { Text("API Settings") },
            text = {
                Column {
                    OutlinedTextField(value = apiKeyInput, onValueChange = { apiKeyInput = it }, label = { Text("OpenRouter Key") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    OutlinedTextField(value = sarvamKeyInput, onValueChange = { sarvamKeyInput = it }, label = { Text("Sarvam AI Key") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.clearHistory(); showSettings = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Clear Chat History", color = Color.White) }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.setApiKey(apiKeyInput); viewModel.setTtsApiKey(sarvamKeyInput); showSettings = false }) { Text("Save") } }
        )
    }
}

// গ্লাসমরফিজম চ্যাট বাবল
@Composable
fun GlassChatBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) Color(0x33E91E63) else Color(0x1AFFFFFF)
    val borderColor = if (msg.isUser) Color(0x55E91E63) else Color(0x33FFFFFF)
    
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(text = msg.text, color = Color.White)
        }
    }
}

// টাইপিং ইন্ডিকেটর (৩টি ডট অ্যানিমেশন)
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "a1")
    val alpha2 by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(400, delayMillis = 150), RepeatMode.Reverse), label = "a2")
    val alpha3 by infiniteTransition.animateFloat(initialValue = 0.2f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(400, delayMillis = 300), RepeatMode.Reverse), label = "a3")

    Row(modifier = Modifier.padding(8.dp).background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("মায়া ভাবছে", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(".", color = Color(0xFFE91E63).copy(alpha = alpha1), fontWeight = FontWeight.Bold)
        Text(".", color = Color(0xFFE91E63).copy(alpha = alpha2), fontWeight = FontWeight.Bold)
        Text(".", color = Color(0xFFE91E63).copy(alpha = alpha3), fontWeight = FontWeight.Bold)
    }
}
