package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
import android.util.Log
import com.krumin.tonguecoinsmanager.domain.model.Platform
import org.json.JSONObject

class FcmDraftRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("fcm_draft_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TITLE = "draft_title"
        private const val KEY_BODY = "draft_body"
        private const val KEY_IMAGE_URL = "draft_imageUrl"
        private const val KEY_DATA_PAYLOAD = "draft_dataPayload"
        private const val KEY_PLATFORMS = "draft_platforms"
        private const val TAG = "FcmDraftRepository"
    }

    data class FcmDraft(
        val title: String,
        val body: String,
        val imageUrl: String?,
        val dataPayload: Map<String, String>,
        val selectedPlatforms: Set<Platform>
    )

    fun saveDraft(draft: FcmDraft) {
        val platformsString = draft.selectedPlatforms.joinToString(",") { it.name }
        val payloadJson = JSONObject(draft.dataPayload).toString()

        prefs.edit()
            .putString(KEY_TITLE, draft.title)
            .putString(KEY_BODY, draft.body)
            .putString(KEY_IMAGE_URL, draft.imageUrl)
            .putString(KEY_DATA_PAYLOAD, payloadJson)
            .putString(KEY_PLATFORMS, platformsString)
            .apply()

        Log.d(TAG, "Draft saved: ${draft.title}")
    }

    fun loadDraft(): FcmDraft? {
        if (!prefs.contains(KEY_TITLE) && !prefs.contains(KEY_IMAGE_URL)) return null
        val title = prefs.getString(KEY_TITLE, "") ?: ""
        val body = prefs.getString(KEY_BODY, "") ?: ""
        val imageUrl = prefs.getString(KEY_IMAGE_URL, null)
        val payloadJson = prefs.getString(KEY_DATA_PAYLOAD, "{}") ?: "{}"
        val platformsString = prefs.getString(KEY_PLATFORMS, "") ?: ""

        val payload = mutableMapOf<String, String>()
        try {
            val json = JSONObject(payloadJson)
            json.keys().forEach { payload[it] = json.getString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading payload JSON", e)
        }

        val platforms = if (platformsString.isEmpty()) {
            Platform.entries.toSet()
        } else {
            platformsString.split(",").mapNotNull {
                try {
                    Platform.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }.toSet()
        }

        return FcmDraft(title, body, imageUrl, payload, platforms)
    }

    fun clearDraft() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Draft cleared")
    }
}
