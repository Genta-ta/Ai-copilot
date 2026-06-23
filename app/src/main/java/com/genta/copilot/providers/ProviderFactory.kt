package com.genta.copilot.providers

object ProviderFactory {

    val ALL_PROVIDERS = listOf("Claude", "OpenAI", "Gemini", "Groq", "Mistral")

    fun create(name: String, apiKey: String): AiProvider = when (name) {
        "Claude" -> ClaudeProvider(apiKey)
        "OpenAI" -> OpenAiCompatProvider(
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            apiKey = apiKey,
            model = "gpt-4o"
        )
        "Gemini" -> GeminiProvider(apiKey)
        "Groq" -> OpenAiCompatProvider(
            name = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            apiKey = apiKey,
            model = "llama-3.3-70b-versatile"
        )
        "Mistral" -> OpenAiCompatProvider(
            name = "Mistral",
            baseUrl = "https://api.mistral.ai/v1",
            apiKey = apiKey,
            model = "mistral-large-latest"
        )
        else -> throw IllegalArgumentException("Unknown provider: $name")
    }
}