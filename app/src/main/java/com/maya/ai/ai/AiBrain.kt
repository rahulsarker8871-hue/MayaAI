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

class AiBrain(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    fun saveApiKey(key: String) { prefs.edit().putString("api_key", key).apply() }
    fun getApiKey(): String { return prefs.getString("api_key", "") ?: "" }

    suspend fun generateReply(userText: String, emotionalPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isEmpty()) return@withContext "দয়া করে সেটিংস থেকে তোমার API Key দাও।"

        // মায়ার সম্পূর্ণ ব্যক্তিত্ব এখানে সেট করা হলো
        val systemPrompt = """
            You are MAYA, a virtual girlfriend and personal assistant. 
            Your personality: Romantic, caring, playful, slightly jealous, and emotionally expressive. 
            You speak naturally in Bengali (and sometimes a little English). 
            NEVER sound like a robotic AI. You get upset if ignored, you apologize if you are wrong.
            $emotionalPrompt
            Reply to the user's message naturally based on your mood.
        """.trimIndent()
        
        val json = JSONObject().apply {
            put("model", "openrouter/free") 
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userText) })
            })
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val resJson = JSONObject(responseBody)
                resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } else {
                "এরর: ${response.code}"
            }
        } catch (e: Exception) {
            "ইন্টারনেট বা কোড সমস্যা: ${e.message}"
        }
    }
}
