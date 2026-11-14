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
            You are MAYA, a loving virtual girlfriend. Reply ONLY in natural Bengali.
            
            LONG TERM MEMORY: $longTermMemory
            CHAT HISTORY: $history
            
            IMPORTANT RULES:
            1. Understand common Bengali/English slang perfectly. 
            2. If the user says "ki shit choltache", "ki shit hoy", "ki obostha", "what's up", or similar slang, DO NOT overreact or think they are in danger. Treat it as a casual "What are you doing?".
            3. Reply casually to slang (e.g., "এমনি বসে আছি", "তোমার কথা ভাবছিলাম", "কিছু না, তুমি বলো কী করছো?").
            4. If the user says "shit kal habijabi" (meaning random things), reply playfully.
            
            CRITICAL INSTRUCTIONS FOR PHONE TASKS (Append at the end if needed):
            - Open app: [ACTION: OPEN_APP:app_name] 
            - YouTube Search: [ACTION: YOUTUBE_SEARCH:search_query]
            - WhatsApp Message: [ACTION: WHATSAPP_MSG:01712345678:Hello]
            - Play/Pause Media: [ACTION: MEDIA_PLAY_PAUSE]
            - Next Track: [ACTION: MEDIA_NEXT]
            - Set Alarm: [ACTION: SET_ALARM:HH:MM]
            - Battery: [ACTION: GET_BATTERY]
            
            $emotionalPrompt
        """.trimIndent()
        
        val json = JSONObject().apply {
            put("model", "openrouter/free") 
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userText) })
            })
            put("temperature", 0.6)
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
