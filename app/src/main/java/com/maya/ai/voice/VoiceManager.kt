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

    suspend fun speakWithSarvam(text: String, apiKey: String, emotion: String = "happy") = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) return@withContext

        val pitch = when (emotion) {
            "happy" -> 0.4f   
            "sad" -> 0.1f
            "angry" -> 0.3f
            else -> 0.2f
        }
        val pace = when (emotion) {
            "happy" -> 1.2f
            "sad" -> 0.9f
            "angry" -> 1.1f
            else -> 1.0f
        }

        val json = JSONObject().apply {
            put("text", text)
            put("target_language_code", "bn-IN")
            put("speaker", "ishita") 
            put("pitch", pitch)
            put("pace", pace)
            put("loudness", 1.5)
            put("model", "bulbul:v1") // ফ্রি এবং সচল মডেল
        }

        val request = Request.Builder()
            .url("https://api.sarvam.ai/text-to-speech")
            .addHeader("api-subscription-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val resJson = JSONObject(responseBody)
                val base64Audio = resJson.getJSONArray("audios").getString(0)
                val audioBytes = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT)

                val audioFile = File(context.cacheDir, "maya_sarvam.wav")
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
            override fun onError(error: Int) { onError("আবার বলো প্লিজ।") }
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
