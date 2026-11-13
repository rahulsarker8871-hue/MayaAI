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
import android.widget.Toast
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
        if (apiKey.isEmpty()) {
            showToast("API Key খালি আছে!")
            return@withContext
        }

        // Sarvam-এর নতুন নিয়ম অনুযায়ী প্যারামিটার
        val json = JSONObject().apply {
            put("text", text)
            put("target_language_code", "bn-IN")
            put("speaker", "anushka") // bulbul:v2 এর জন্য মিষ্টি মেয়েলি ভয়েস
            put("model", "bulbul:v2")   // আপডেট করা মডেল
            put("pace", 1.1)           // কথা বলার গতি (স্বাভাবিকের চেয়ে একটু দ্রুত)
            // pitch এবং loudness শুধু bulbul:v2 তে কাজ করে, তাই সেগুলো রাখা হলো
            put("pitch", 0.2) 
            put("loudness", 1.2)
        }

        val request = Request.Builder()
            .url("https://api.sarvam.ai/text-to-speech")
            .addHeader("api-subscription-key", apiKey.trim())
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

                withContext(Dispatchers.Main) {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        prepare()
                        start()
                    }
                }
            } else {
                // যদি এখনো এরর আসে, তবে স্ক্রিনে দেখাবে
                showToast("Sarvam Error: ${response.code} - $responseBody")
            }
        } catch (e: Exception) {
            showToast("Network Error: ${e.message}")
        }
    }

    private suspend fun showToast(msg: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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
