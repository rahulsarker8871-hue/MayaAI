package com.maya.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.maya.ai.phone.PhoneController

class MayaBackgroundService : Service() {
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var phoneController: PhoneController

    override fun onCreate() {
        super.onCreate()
        phoneController = PhoneController(this)
        startForegroundService()
        setupSpeechRecognizer()
    }

    private fun startForegroundService() {
        val channelId = "maya_bg_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Maya Background Listener", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("বেবি তোমার কথা শুনছে...")
            .setContentText("আমাকে ডাকতে 'বেবি' বলে ডাকো।")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                speechRecognizer?.startListening(intent)
            }

            override fun onError(error: Int) {
                speechRecognizer?.startListening(intent)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0)?.lowercase() ?: ""
                // "বেবি" বা "Baby" ডাকলে সাড়া দেবে
                if (text.contains("বেবি") || text.contains("baby")) {
                    handleCommand(text)
                }
                speechRecognizer?.startListening(intent)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0)?.lowercase() ?: ""
                if (text.contains("বেবি") || text.contains("baby")) {
                    handleCommand(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun handleCommand(text: String) {
        if (text.contains("গান বন্ধ") || text.contains("পজ") || text.contains("অফ") || text.contains("থামাও") || text.contains("stop")) {
            phoneController.togglePlayPause()
        } else if (text.contains("নেক্সট গান") || text.contains("next")) {
            phoneController.nextTrack()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
