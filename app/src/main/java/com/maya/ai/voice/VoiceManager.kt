package com.maya.ai.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class VoiceManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun speakWithFishAudio(text: String, apiKey: String) = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext
        
        val json = JSONObject().apply {
            put("text", text)
            put("model", "s2.1-pro-free")
            // Young Bengali Voice (মিষ্টি তরুণী কণ্ঠস্বর)
            put("voice_id", "5e984cb0bd3b48db94961da04b341464") 
            put("format", "mp3")
        }

        val request = Request.Builder()
            .url("https://api.fish.audio/v1/tts")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val audioBytes = response.body?.bytes()
                if (audioBytes != null) {
                    val audioFile = File(context.cacheDir, "maya_fish_voice.mp3")
                    FileOutputStream(audioFile).use { it.write(audioBytes) }
                    
                    Handler(Looper.getMainLooper()).post {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioFile.absolutePath)
                            prepare()
                            start()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN")
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onError("আবার বলো প্লিজ, ঠিকমতো শুনতে পাইনি।") }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) onResult(matches[0])
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() { speechRecognizer?.stopListening() }
    fun destroy() { speechRecognizer?.destroy(); mediaPlayer?.release() }
}
