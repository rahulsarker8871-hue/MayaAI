package com.maya.ai.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VoiceManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    
    // Microsoft Edge TTS-এর পাবলিক WebSocket URL
    private val edgeTtsUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"

    init {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Edge TTS-এর মাধ্যমে টেক্সট থেকে অডিও জেনারেট ও প্লে করা
    suspend fun speakWithEdgeTts(text: String) = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val config = JSONObject().apply {
            put("context", JSONObject().apply {
                put("synthesis", JSONObject().apply {
                    put("audio", JSONObject().apply {
                        put("metadataoptions", JSONObject().apply {
                            put("sentenceBoundaryEnabled", "false")
                            put("wordBoundaryEnabled", "false")
                        })
                        put("outputFormat", "audio-24khz-48kbitrate-mono-mp3")
                    })
                })
            })
        }

        // বাংলা নিউরাল ভয়েস (bn-BD-NabanitaNeural - মেয়েলি কণ্ঠ)
        val ssml = """
            <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='bn-BD'>
                <voice name='bn-BD-NabanitaNeural'>
                    <prosody rate='0%' pitch='0Hz'>$text</prosody>
                </voice>
            </speak>
        """.trimIndent()

        val request = Request.Builder()
            .url(edgeTtsUrl)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        try {
            val webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    // কনফিগারেশন মেসেজ পাঠানো
                    webSocket.send("Path: speech.config\r\nX-RequestId: ${UUID.randomUUID()}\r\nX-Timestamp: ${System.currentTimeMillis()}\r\nContent-Type: application/json; charset=utf-8\r\n\r\n$config")
                    // SSML মেসেজ পাঠানো
                    val ssmlId = UUID.randomUUID().toString().replace("-", "")
                    webSocket.send("Path: ssml\r\nX-RequestId: $ssmlId\r\nX-Timestamp: ${System.currentTimeMillis()}\r\nContent-Type: application/ssml+xml\r\n\r\n$ssml")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Edge TTS টেক্সট মেসেজে বাইনারি ডেটা পাঠায় না, বাইনারি ফ্রেমে পাঠায়
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteArray) {
                    // প্রাপ্ত বাইনারি অডিও ডেটা ফাইলে সেভ করা
                    val audioFile = File(context.cacheDir, "maya_edge_tts.mp3")
                    FileOutputStream(audioFile).use { it.write(bytes) }

                    // মেইন থ্রেডে অডিও প্লে করা
                    withContext(Dispatchers.Main) {
                        mediaPlayer?.release()
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(audioFile.absolutePath)
                            prepare()
                            start()
                        }
                    }
                    webSocket.close(1000, "Done")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    t.printStackTrace()
                    // ব্যর্থ হলে ডিফল্ট TTS দিয়ে চেষ্টা করা (ফলব্যাক)
                    fallbackToAndroidTts(text)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ফলব্যাক: অ্যান্ড্রয়েডের ডিফল্ট TTS
    private fun fallbackToAndroidTts(text: String) {
        // (এখানে আগের TTS কোড থাকবে, যা ব্যর্থ হলে কাজ করবে)
        // ...
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
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

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        mediaPlayer?.release()
    }
}
