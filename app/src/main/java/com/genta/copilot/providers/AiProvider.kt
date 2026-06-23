package com.genta.copilot.providers

interface AiProvider {
    val name: String
    suspend fun complete(systemPrompt: String, userMessage: String): String
}