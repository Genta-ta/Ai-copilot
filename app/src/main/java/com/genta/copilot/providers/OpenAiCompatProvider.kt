
package com.genta.copilot.providers

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiCompatProvider(
    override val name: String,
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun complete(systemPrompt: String, userMessage: String): String {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 2048)
            put("messages", messages)
        }.toString()

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response from $name")

        if (!response.isSuccessful) {
            throw Exception("$name error ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        return json
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }
}