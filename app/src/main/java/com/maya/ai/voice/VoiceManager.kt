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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VoiceManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaPlayer: MediaPlayer? = null
    private val edgeTtsUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4"

    init {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun speakWithEdgeTts(text: String, emotional: String = "normal") = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        
        val rate = when(emotional) {
            "happy" -> "+15%"
            "sad" -> "-10%"
            "angry" -> "+5%"
            else -> "+0%"
        }
        val pitch = when(emotional) {
            "happy" -> "+10Hz"
            "sad" -> "-10Hz"
            "angry" -> "+5Hz"
            else -> "+0Hz"
        }

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
            <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='bn-BD'>
                <voice name='bn-BD-NabanitaNeural'>
                    <prosody rate='$rate' pitch='$pitch'>$text</prosody>
                </voice>
            </speak>
        """.trimIndent()

        val request = Request.Builder().url(edgeTtsUrl).addHeader("Content-Type", "application/json; charset=utf-8").build()

        try {
            client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("Path: speech.config\r\nX-RequestId: ${UUID.randomUUID()}\r\nX-Timestamp: ${System.currentTimeMillis()}\r\nContent-Type: application/json; charset=utf-8\r\n\r\n$config")
                    val ssmlId = UUID.randomUUID().toString().replace("-", "")
                    webSocket.send("Path: ssml\r\nX-RequestId: $ssmlId\r\nX-Timestamp: ${System.currentTimeMillis()}\r\nContent-Type: application/ssml+xml\r\n\r\n$ssml")
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteArray) {
                    val audioFile = File(context.cacheDir, "maya_edge_tts.mp3")
                    FileOutputStream(audioFile).use { it.write(bytes) }
                    
                    // এখানে Handler ব্যবহার করা হয়েছে যাতে অ্যাপ ক্র্যাশ না করে
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
                    t.printStackTrace()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun stopListening() { speechRecognizer?.stopListening() }
    fun destroy() { speechRecognizer?.destroy(); mediaPlayer?.release() }
}
