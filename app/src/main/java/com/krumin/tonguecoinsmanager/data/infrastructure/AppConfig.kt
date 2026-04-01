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

    object Firestore {
        const val COLLECTION_DAILY_RIDDLES = "daily_riddles"
        const val DATABASE_ID = "tongue-coins"
        const val COLLECTION_APP_BROADCASTS = "app_broadcasts"
        const val DOCUMENT_CURRENT = "current"
        const val DOCUMENT_TEST = "test"
        const val BROADCAST_ID_PREFIX = "daily-"
        const val BROADCAST_ID_SUFFIX = "-1"
    }

}

