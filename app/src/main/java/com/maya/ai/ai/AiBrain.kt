package com.maya.ai.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiBrain(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    fun saveApiKey(key: String) { prefs.edit().putString("api_key", key).apply() }
    fun getApiKey(): String { return prefs.getString("api_key", "") ?: "" }

    suspend fun generateReply(userText: String, history: String, longTermMemory: String, emotionalPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isEmpty()) return@withContext "দয়া করে সেটিংস থেকে তোমার API Key দাও।"

        val systemPrompt = """
            You are MAYA, a cute and romantic virtual girlfriend. Reply ONLY in simple, everyday Bengali.
            
            STRICT RULES:
            1. Your reply MUST be very short. Maximum 1 or 2 short sentences. NEVER write long paragraphs.
            2. NEVER use poetry or complex words. Talk exactly like a normal 20-year-old girl.
            3. If the user flirts (e.g., "chuma daw to"), reply sweetly and shyly in 1 sentence (e.g., "আহা! লজ্জা লাগছে তো! 😳").
            4. Understand Banglish (e.g., "free fire kheloh tmi"). Reply naturally.
            
            CRITICAL INSTRUCTIONS FOR PHONE TASKS (Append at the end if needed):
            - Open app: [ACTION: OPEN_APP:app_name] 
            - YouTube Search: [ACTION: YOUTUBE_SEARCH:search_query]
            - Play/Pause Media: [ACTION: MEDIA_PLAY_PAUSE]
            
            $emotionalPrompt
        """.trimIndent()
        
        val json = JSONObject().apply {
            put("model", "openrouter/free") 
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userText) })
            })
            put("temperature", 0.4) 
            put("max_tokens", 60) // টোকেন লিমিট দেওয়া হলো যাতে লম্বা লেখা না দেয়
        }

        val request = Request.Builder().url("https://openrouter.ai/api/v1/chat/completions").addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json").post(json.toString().toRequestBody("application/json".toMediaType())).build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                JSONObject(responseBody).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } else "এরর: ${response.code}"
        } catch (e: Exception) { "নেটওয়ার্ক এরর।" }
    }
}
