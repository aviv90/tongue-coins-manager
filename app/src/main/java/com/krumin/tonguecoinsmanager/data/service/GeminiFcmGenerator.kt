package com.krumin.tonguecoinsmanager.data.service

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.infrastructure.PromptTemplates
import com.krumin.tonguecoinsmanager.domain.service.FcmGenerator
import com.krumin.tonguecoinsmanager.domain.service.GeneratedFcmContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GeminiFcmGenerator(
    private val apiKey: String
) : FcmGenerator {

    companion object {
        private const val TAG = "GeminiFcmGenerator"
    }

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = AppConfig.Gemini.MODEL_FLASH,
            generationConfig = generationConfig {
                temperature = 1.0f
                topP = 0.95f
                topK = 40
                responseMimeType = "application/json"
            }
        )
    }

    override suspend fun generateContent(idea: String): GeneratedFcmContent? =
        withContext(Dispatchers.IO) {
            val prompt = PromptTemplates.getFcmGenerationPrompt(idea)
            try {
                val response = model.generateContent(prompt)
                val jsonStr = response.text?.let { cleanJson(it) } ?: run {
                    Log.e(TAG, "Response text is null")
                    return@withContext null
                }

                val jsonElement = Json.parseToJsonElement(jsonStr).jsonObject
                GeneratedFcmContent(
                    title = jsonElement["title"]?.jsonPrimitive?.content ?: "",
                    body = jsonElement["body"]?.jsonPrimitive?.content ?: ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating content: ${e.message}", e)
                null
            }
        }

    private fun cleanJson(input: String): String {
        return input.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
