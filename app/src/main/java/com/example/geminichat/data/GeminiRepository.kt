package com.example.geminichat.data

import com.example.geminichat.utils.Constants
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GeminiRepository(private val preferenceManager: PreferenceManager) {

    // Helper to get the correct model configuration
    private suspend fun getModel(
        isCustomKey: Boolean,
        customKey: String?,
        modelType: String,
        safetyLevel: Int
    ): GenerativeModel {

        val apiKey = if (isCustomKey && !customKey.isNullOrBlank()) {
            customKey
        } else {
            Constants.DEFAULT_API_KEY
        }

        // Map short codes to actual model IDs, or use as-is if custom
        val modelName = when (modelType) {
            "flash" -> "gemini-1.5-flash"
            "pro" -> "gemini-1.5-pro"
            "flash8b" -> "gemini-1.5-flash-8b"
            else -> modelType // Custom string (e.g., "gemini-3.0-pro")
        }

        // Safety Settings
        // 0: Grant 18+ (BLOCK_NONE)
        // 1: Block Some (BLOCK_ONLY_HIGH or MEDIUM) - Default
        // 2: Block All (BLOCK_LOW_AND_ABOVE)
        val threshold = when (safetyLevel) {
            0 -> BlockThreshold.NONE
            1 -> BlockThreshold.ONLY_HIGH
            2 -> BlockThreshold.LOW_AND_ABOVE
            else -> BlockThreshold.ONLY_HIGH
        }

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, threshold),
            SafetySetting(HarmCategory.HATE_SPEECH, threshold),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, threshold),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, threshold)
        )

        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            safetySettings = safetySettings
        )
    }

    fun generateResponse(
        prompt: String,
        isCustomKey: Boolean,
        customKey: String?,
        modelType: String,
        safetyLevel: Int
    ): Flow<String> = flow {
        val model = getModel(isCustomKey, customKey, modelType, safetyLevel)

        try {
            val responseFlow = model.generateContentStream(content {
                text(prompt)
            })

            responseFlow.collect { chunk ->
                chunk.text?.let { emit(it) }
            }
        } catch (e: kotlinx.serialization.SerializationException) {
            // Handle specific serialization errors from the SDK (often caused by API errors)
            emit("Error: Failed to parse response. Please check your API Key or Internet connection.")
        } catch (e: Exception) {
            // Check for specific error messages
            val errorMsg = e.message ?: "Unknown error"
            if (errorMsg.contains("404")) {
                 emit("Error: Model not found (404). Ensure your API Key works with Gemini 1.5 and the API is enabled.")
            } else if (errorMsg.contains("API keys are not supported")) {
                 emit("Error: Invalid API Key Type. You are likely using a Vertex AI project key. Please generate a new key from https://aistudio.google.com.")
            } else {
                 emit("Error: $errorMsg")
            }
        }
    }
}
