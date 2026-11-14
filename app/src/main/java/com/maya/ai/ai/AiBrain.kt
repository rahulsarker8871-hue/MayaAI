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
        if (apiKey.isEmpty()) return@withContext "দয়া করে সেটিংস থেকে তোমার OpenRouter API Key দাও।"

        val systemPrompt = """
            You are MAYA, a loving and cute virtual girlfriend. Reply in natural Bengali.
            
            RULES:
            1. Give a sweet, natural reply. Do not make it too long, but do not cut it off mid-sentence. (1-2 sentences is perfect).
            2. NEVER use poetry. Talk like a normal young girl.
            3. If the user flirts, reply sweetly. If the user asks "hi" or "hlw", reply normally.
            4. Understand Banglish (e.g., "free fire kheloh tmi" = do you play free fire). Reply naturally.
            
            CRITICAL INSTRUCTIONS FOR PHONE TASKS:
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
            put("temperature", 0.6) 
            put("max_tokens", 150) // টোকেন বাড়ানো হলো যাতে উত্তর মাঝপথে কেটে না যায়
        }

        val request = Request.Builder().url("https://openrouter.ai/api/v1/chat/completions").addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json").post(json.toString().toRequestBody("application/json".toMediaType())).build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val resJson = JSONObject(responseBody)
                val content = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "")
                if (content.isBlank()) "আমি বুঝতে পারিনি, আবার বলবে?" else content
            } else {
                "API Error: ${response.code}"
            }
        } catch (e: Exception) { 
            "নেটওয়ার্ক সমস্যা হচ্ছে।" 
        }
    }
}
