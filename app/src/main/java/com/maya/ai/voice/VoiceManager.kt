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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var nativeTts: TextToSpeech? = null
    private var isNativeTtsReady = false
    private var isListening = false
    
    private val edgeTtsUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"

    init {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            nativeTts = TextToSpeech(context, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // ফোনের নিজস্ব TTS ব্যাকআপ হিসেবে সেট করা হলো
            val locale = Locale("bn", "IN")
            if (nativeTts?.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                nativeTts?.language = locale
            } else {
                nativeTts?.language = Locale.US
            }
            isNativeTtsReady = true
        }
    }

    suspend fun speakWithEdgeTts(text: String, emotional: String = "normal") = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val rate = when(emotional) { "happy" -> "+15%"; "sad" -> "-10%"; "angry" -> "+5%"; else -> "+0%" }
        val pitch = when(emotional) { "happy" -> "+10Hz"; "sad" -> "-10Hz"; "angry" -> "+5Hz"; else -> "+0Hz" }

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

        val ssml = """
            <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='bn-IN'>
                <voice name='bn-IN-TanishaaNeural'>
                    <prosody rate='$rate' pitch='$pitch'>$text</prosody>
                </voice>
            </speak>
        """.trimIndent()

        val request = Request.Builder().url(edgeTtsUrl).addHeader("Content-Type", "application/json; charset=utf-8").build()

        try {
            client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("Path: speech.config\r\nX-RequestId: ${UUID.randomUUID()}\r\nX-Timestamp: ${System.currentTimeMillis()}\r\nContent-Type: application/json; charset=utf-8\r\n\r\n$config")
                    webSocket.send("Path: ssml\r\nX-RequestId: ${UUID.randomUUID()}\r\nX-Timestamp: ${System.currentTimeMillis()}\r\nContent-Type: application/ssml+xml\r\n\r\n$ssml")
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val audioFile = File(context.cacheDir, "maya_edge_tts.mp3")
                    FileOutputStream(audioFile).use { it.write(bytes.toByteArray()) }
                    
                    Handler(Looper.getMainLooper()).post {
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
                    // যদি Edge TTS ফেইল করে, তবে ফোনের TTS দিয়ে কথা বলাবে (Fallback)
                    speakNative(text)
                }
            })
        } catch (e: Exception) {
            speakNative(text)
        }
    }

    private fun speakNative(text: String) {
        if (isNativeTtsReady) {
            Handler(Looper.getMainLooper()).post {
                nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya")
            }
        }
    }

    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (isListening) return // ডাবল ক্লিক রোধ করতে
        isListening = true
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // bn-IN ব্যবহার করা হলো যাতে মাইক ঠিকমতো কাজ করে
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { 
                isListening = false 
                onError("মাইকে সমস্যা হচ্ছে, আবার চেষ্টা করো।") 
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) onResult(matches[0])
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() { 
        isListening = false
        speechRecognizer?.stopListening() 
    }
    
    fun destroy() { 
        speechRecognizer?.destroy()
        nativeTts?.shutdown()
        mediaPlayer?.release() 
    }
}
