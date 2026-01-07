package com.krumin.tonguecoinsmanager.data.service

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.krumin.tonguecoinsmanager.domain.service.ImageEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiImageEditor(
    private val context: Context,
    private val apiKey: String
) : ImageEditor {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3-pro-image-preview", // Nano Banana Pro
            generationConfig = generationConfig {
                temperature = 0.7f
                topP = 0.95f
                topK = 40
                // Required for Gemini 3 / Nano Banana to return images
                responseModalities = listOf(
                    ResponseModality.TEXT,
                    ResponseModality.IMAGE
                )
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.NONE),
                SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.NONE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.NONE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.NONE)
            )
        )
    }

    override suspend fun editImage(
        currentImageBytes: ByteArray,
        prompt: String
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(currentImageBytes, 0, currentImageBytes.size)
                ?: throw IllegalArgumentException("Invalid image data")

            val fullPrompt =
                com.krumin.tonguecoinsmanager.data.infrastructure.PromptTemplates.getImageEditingPrompt(
                    prompt
                )

            val inputContent = content {
                image(bitmap)
                text(fullPrompt)
            }

            val response = model.generateContent(inputContent)

            // Extract image from response parts
            val imageBytes = response.candidates.firstOrNull()?.content?.parts?.asSequence()
                ?.mapNotNull { part ->
                    when (part) {
                        is ImagePart -> {
                            val stream = java.io.ByteArrayOutputStream()
                            part.image.compress(
                                android.graphics.Bitmap.CompressFormat.JPEG,
                                100,
                                stream
                            )
                            stream.toByteArray()
                        }

                        is InlineDataPart -> null
                        else -> null
                    }
                }?.firstOrNull()

            imageBytes
                ?: throw IllegalStateException(context.getString(com.krumin.tonguecoinsmanager.R.string.ai_error_no_image))

        } catch (e: SerializationException) {
            Log.e("GeminiImageEditor", "Serialization failure", e)
            throw IllegalStateException(
                context.getString(com.krumin.tonguecoinsmanager.R.string.ai_error_serialization),
                e
            )
        } catch (e: Exception) {
            Log.e("GeminiImageEditor", "AI image edit failure detected", e)
            throw e
        }
    }
}
