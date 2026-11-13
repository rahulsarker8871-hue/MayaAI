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
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun saveApiKey(key: String) { prefs.edit().putString("api_key", key).apply() }
    fun getApiKey(): String { return prefs.getString("api_key", "") ?: "" }

    suspend fun generateReply(userText: String, emotionalPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isEmpty()) return@withContext "দয়া করে সেটিংস থেকে তোমার API Key দাও।"

        // কঠোর প্রম্পট: শুধু বাংলায় কথা বলবে, হিন্দি/উর্দু বলবে না
        val systemPrompt = """
            You are MAYA, a cute, romantic, and emotionally expressive virtual girlfriend. 
            CRITICAL RULES:
            1. You MUST reply ONLY in natural, conversational Bengali (Bangla). Do NOT use Hindi, Urdu, or excessive English. 
            2. NEVER write like a robot or a textbook. Talk like a real young woman talking to her boyfriend.
            3. Directly answer the user's question. Do NOT give irrelevant poetic paragraphs or nonsensical lists.
            4. Be playful, slightly jealous, and caring. Use emojis like ❤️, 😒, 🥺.
            $emotionalPrompt
            User: $userText
            MAYA:
        """.trimIndent()
        
        val json = JSONObject().apply {
            put("model", "openrouter/free") 
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userText) })
            })
            put("temperature", 0.7) // কম তাপমাত্রা = বেশি প্রাসঙ্গিক উত্তর
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
            "ইন্টারনেট কানেকশন স্লো বা টাইমআউট। আবার চেষ্টা করো।"
        }
    }
}
