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
import android.speech.tts.TextToSpeech
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var nativeTts: TextToSpeech? = null
    private var isNativeTtsReady = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            nativeTts = TextToSpeech(context, this)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("bn", "IN")
            if (nativeTts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                nativeTts?.language = locale
            } else { nativeTts?.language = Locale.US }
            isNativeTtsReady = true
        }
    }

    suspend fun speakWithSarvam(text: String, apiKey: String, emotion: String = "happy") = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            showToast("Sarvam AI Key সেভ করা নেই! ফোনের ভয়েস ব্যবহার হচ্ছে।")
            speakNative(text)
            return@withContext
        }

        val json = JSONObject().apply {
            put("text", text)
            put("target_language_code", "bn-IN")
            put("speaker", "anushka") 
            put("model", "bulbul:v2")
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
                showToast("Sarvam Error: ${response.code}. ফোনের ভয়েস ব্যবহার হচ্ছে।")
                speakNative(text)
            }
        } catch (e: Exception) {
            showToast("Network Error. ফোনের ভয়েস ব্যবহার হচ্ছে।")
            speakNative(text)
        }
    }

    private suspend fun showToast(msg: String) {
        withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    fun speakNative(text: String) {
        if (isNativeTtsReady) {
            Handler(Looper.getMainLooper()).post {
                nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya")
            }
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
    fun destroy() { speechRecognizer?.destroy(); mediaPlayer?.release(); nativeTts?.shutdown() }
}
