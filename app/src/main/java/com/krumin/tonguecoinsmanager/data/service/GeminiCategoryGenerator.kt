package com.krumin.tonguecoinsmanager.data.service

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.generationConfig
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class GeminiCategoryGenerator(
    private val apiKey: String
) : CategoryGenerator {

    private val model = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.45f
            topP = 0.95f
            topK = 40
            responseMimeType = "application/json"
        }
    )

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
        // Diversify context by sampling existing categorization patterns
        val examples = contextPhotos
            .filter { it.categories.isNotBlank() }
            .shuffled()
            .take(12)
            .joinToString("\n") {
                "ביטוי: ${it.title} -> קטגוריות: ${it.categories}"
            }

        return """
            אתה מומחה עילית לשפה העברית, מטבעות לשון (Idioms), תרבות ישראלית וסלנג.
            תפקידך להציע קטגוריות רלוונטיות, קצרות וחכמות עבור ביטוי (מטבע לשון) נתון.
            
            הנחיות:
            - החזר לכל היותר 2 קטגוריות רלוונטיות.
            - הקטגוריות חייבות להיות תמציתיות (1-3 מילים).
            - השתמש בעברית בלבד.
            - התמקד בתמות כמו: "סלנג", "יהדות", "צבא", "אוכל", "היסטוריה", "מוזיקה", "רגשות" וכו'.
            - המטרה היא לעזור למשתמש למצוא ולארגן את התמונות לפי נושאים.
            
            פורמט פלט:
            - החזר את התשובה כמערך JSON של מחרוזות בלבד.
            - דוגמה: ["סלנג עברי", "משפחה"]
            
            הקשר מהמאגר הקיים (דוגמאות לסגנון):
            ${examples}
            
            משימה נוכחית:
            ביטוי: $title
            החזר אך ורק את מערך ה-JSON (מקסימום 2 פריטים).
        """.trimIndent()
    }
}
