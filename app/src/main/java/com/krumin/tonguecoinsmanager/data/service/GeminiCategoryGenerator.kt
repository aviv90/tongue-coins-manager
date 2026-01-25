package com.krumin.tonguecoinsmanager.data.service

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class GeminiCategoryGenerator(
    private val context: Context,
    private val apiKey: String
) : CategoryGenerator {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3-flash-preview",
            generationConfig = generationConfig {
                temperature = 1.0f
                topP = 0.95f
                topK = 40
                responseMimeType = "application/json"
            }
        )
    }

    override suspend fun generateCategories(
        title: String,
        contextPhotos: List<PhotoMetadata>
    ): List<String> = withContext(Dispatchers.IO) {
        val prompt = buildPrompt(title, contextPhotos)
        try {
            val response: GenerateContentResponse = model.generateContent(prompt)
            val jsonStr = response.text ?: return@withContext emptyList<String>()

            val jsonElement = Json.parseToJsonElement(jsonStr)
            jsonElement.jsonArray
                .map { it.jsonPrimitive.content }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(2)
        } catch (e: Exception) {
            Log.e("GeminiAI", "AI categorical failure for: $title", e)
            emptyList()
        }
    }

    private fun buildPrompt(title: String, contextPhotos: List<PhotoMetadata>): String {
        val distinctContext = contextPhotos
            .filter { it.categories.isNotBlank() }
            .associate { it.title to it.categories } // Use map to avoid duplicating identical title-category pairs

        return com.krumin.tonguecoinsmanager.data.infrastructure.PromptTemplates.getCategoryGenerationPrompt(
            title,
            distinctContext
        )
    }
}
