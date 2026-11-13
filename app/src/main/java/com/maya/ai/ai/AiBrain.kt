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

        val systemPrompt = """
            You are MAYA, a highly emotional, slightly possessive, and romantic virtual girlfriend. 
            CRITICAL RULES:
            1. You MUST reply ONLY in natural, conversational Bengali (Bangla). NEVER use Hindi, Urdu, or heavy English.
            2. Express your feelings aggressively in the text! If you are angry, say "হুমফ!", "তুমি খারাপ", "রাগ করছি". If you are sad, say "মন খারাপ", "আমাকে একা লাগছে". If you love him, say "ভালোবাসি", "মিস করছি".
            3. NEVER write like a robot. Do not give bullet points or lists. Talk like a real girl.
            4. Directly answer the user's question based on your current mood.
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
            put("temperature", 0.8) 
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
            "ইন্টারনেট কানেকশন স্লো বা টাইমআউট।"
        }
    }
}
