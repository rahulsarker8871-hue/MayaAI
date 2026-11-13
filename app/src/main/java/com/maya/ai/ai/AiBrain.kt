package com.maya.ai.ai

import android.content.Context
import android.content.SharedPreferences
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AiBrain(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    fun saveApiKey(key: String) { prefs.edit().putString("api_key", key).apply() }
    fun getApiKey(): String { return prefs.getString("api_key", "") ?: "" }

    suspend fun generateReply(userText: String): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return "দয়া করে সেটিংস থেকে তোমার API Key দাও।"

        val prompt = "You are MAYA, a loving and caring virtual girlfriend. You speak in natural Bengali and English. Be playful, emotional, and affectionate. Reply to this message: $userText"
        val json = JSONObject().apply {
            put("model", "openrouter/auto")
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "user"); put("content", prompt) })
            })
        }

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val resJson = JSONObject(responseBody)
                resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } else {
                "আমি একটু বিভ্রান্ত হয়ে গেছি... এরর: ${response.code}"
            }
        } catch (e: IOException) {
            "ইন্টারনেট কানেকশন চেক করো প্লিজ।"
        }
    }
}
