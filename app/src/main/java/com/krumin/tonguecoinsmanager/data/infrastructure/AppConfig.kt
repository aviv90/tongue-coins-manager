package com.krumin.tonguecoinsmanager.data.infrastructure

object AppConfig {
    object Gcs {
        const val PRIVATE_BUCKET = "tonguecoins-private"
        const val PUBLIC_BUCKET = "tonguecoins"
        const val BASE_URL = "https://storage.googleapis.com"
        const val DEFAULT_JSON_FILE = "content.json"
        const val DEFAULT_KEY_FILE = "gcp-key.json"

        // Metadata
        const val ID_PREFIX = "img"
        const val IMAGE_EXT = ".jpeg"
        const val CONTENT_TYPE_JPEG = "image/jpeg"
        const val CONTENT_TYPE_JSON = "application/json"

        // Temp files
        const val TEMP_PREFIX_UPLOAD = "upload"
        const val TEMP_PREFIX_AI = "ai_edit_"
        const val EXTENSION_JPG = ".jpg"
        const val MIME_TYPE_IMAGE = "image/*"
        const val SEPARATOR_COMMA = ", "

        // Local storage
        const val DOWNLOAD_FOLDER = "/TongueCoins"
    }

    object Navigation {
        const val ROUTE_LIST = "list"
        const val ROUTE_EDIT_BASE = "edit"
        const val ARG_ID = "id"
        const val ROUTE_EDIT_FULL = "$ROUTE_EDIT_BASE?$ARG_ID={$ARG_ID}"

        const val KEY_RESULT = "action_result"
        const val RESULT_ADD = "added"
        const val RESULT_EDIT = "edited"
        const val RESULT_DELETE = "deleted"
    }
}
