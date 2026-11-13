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

    suspend fun generateReply(userText: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isEmpty()) return@withContext "দয়া করে সেটিংস থেকে তোমার API Key দাও।"

        val prompt = "You are MAYA, a loving, caring, and playful virtual girlfriend. You speak in natural Bengali. Be affectionate and emotionally expressive. Reply to: $userText"
        
        val json = JSONObject().apply {
            // OpenRouter Free Models Router - Automatically selects an available free model
            put("model", "openrouter/free") 
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

        return@withContext try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val resJson = JSONObject(responseBody)
                resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            } else {
                "এরর: ${response.code} - $responseBody"
            }
        } catch (e: Exception) {
            "ইন্টারনেট বা কোড সমস্যা: ${e.message}"
        }
    }
}
