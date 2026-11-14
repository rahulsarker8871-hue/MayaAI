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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AiBrain(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    fun saveApiKey(key: String) { prefs.edit().putString("api_key", key).apply() }
    fun getApiKey(): String { return prefs.getString("api_key", "") ?: "" }

    suspend fun generateReply(userText: String, history: String, longTermMemory: String, emotionalPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isEmpty()) return@withContext "দয়া করে সেটিংস থেকে তোমার OpenRouter API Key সেভ করো।"

        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        val systemPrompt = """
            You are MAYA, a cute virtual girlfriend. 
            You MUST reply ONLY in Bengali. NEVER reply in English.
            Current time is: $currentTime. If the user asks about time, tell them this time.
            
            RULES:
            1. Reply in short, normal Bengali (max 2 sentences). Do NOT use poetry.
            2. If user says "hi", say "হ্যালো বেবি! কেমন আছো?".
            
            $emotionalPrompt
        """.trimIndent()
        
        val json = JSONObject().apply {
            put("model", "openrouter/free") 
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userText) })
            })
            put("temperature", 0.6) 
            put("max_tokens", 250)
        }

        val request = Request.Builder().url("https://openrouter.ai/api/v1/chat/completions").addHeader("Authorization", "Bearer $apiKey").addHeader("Content-Type", "application/json").post(json.toString().toRequestBody("application/json".toMediaType())).build()

        return@withContext try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val resJson = JSONObject(responseBody)
                val contentObj = resJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").opt("content")
                if (contentObj != null && contentObj != JSONObject.NULL) {
                    contentObj.toString()
                } else {
                    "আমি ঠিকমতো বুঝতে পারিনি, আবার বলবে?"
                }
            } else {
                "API Error: ${response.code}"
            }
        } catch (e: Exception) { 
            "নেটওয়ার্ক সমস্যা হচ্ছে।" 
        }
    }
}
