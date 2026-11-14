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
            1. Understand common Bengali/English slang (e.g., "ki shit choltache" means "what's going on", "ki obostha" means "how are you"). Answer naturally without overreacting.
            2. Do NOT assume the user is screaming or in danger unless they explicitly say so.
            3. If the user asks "what are you doing" or "what's going on", reply normally (e.g., "তোমার সাথে কথা বলছি", "তোমার কথা ভাবছিলাম").
            
            CRITICAL INSTRUCTIONS FOR PHONE TASKS (Append at the end if needed):
            - Open app: [ACTION: OPEN_APP:app_name] (e.g., youtube, spotify, gallery)
            - YouTube Search: [ACTION: YOUTUBE_SEARCH:search_query]
            - WhatsApp Message: [ACTION: WHATSAPP_MSG:01712345678:Hello]
            - Play/Pause Media: [ACTION: MEDIA_PLAY_PAUSE]
            - Next Track: [ACTION: MEDIA_NEXT]
            - Set Alarm: [ACTION: SET_ALARM:HH:MM]
            - Battery: [ACTION: GET_BATTERY]
            - Call: [ACTION: CALL:01712345678]
            
            $emotionalPrompt
        """.trimIndent()
        
        val json = JSONObject().apply {
            put("model", "openrouter/free") 
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userText) })
            })
            put("temperature", 0.6) // তাপমাত্রা কমানো হলো যাতে বেশি বাড়াবাড়ি না করে
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
