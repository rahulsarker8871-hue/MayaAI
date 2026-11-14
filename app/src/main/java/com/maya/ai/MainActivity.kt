package com.maya.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.maya.ai.ui.MayaChatScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MayaViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkOverlayPermission()
        } else {
            Toast.makeText(this, "সব পারমিশন দরকার!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CALL_PHONE, Manifest.permission.SEND_SMS)
        }

        val missingPermissions = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            checkOverlayPermission()
        }

        setContent { MayaChatScreen(viewModel = viewModel) }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "ফ্লোটিং বাবলের জন্য 'Display over other apps' পারমিশন দাও!", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            startServices()
        }
    }

    private fun startServices() {
        // ব্যাকগ্রাউন্ড ভয়েস সার্ভিস
        val bgIntent = Intent(this, MayaBackgroundService::class.java)
        // ফ্লোটিং বাবল সার্ভিস
        val bubbleIntent = Intent(this, FloatingBubbleService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(bgIntent)
            startForegroundService(bubbleIntent)
        } else {
            startService(bgIntent)
            startService(bubbleIntent)
        }
    }
}
