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
        const val COLLECTION_SCHEDULEED_FCM = "scheduled_fcm"
        const val DOCUMENT_CURRENT = "current"
        const val DOCUMENT_TEST = "test"
        const val BROADCAST_ID_PREFIX = "daily-"
        const val BROADCAST_ID_SUFFIX = "-1"
    }

    object Gemini {
        const val MODEL_FLASH = "gemini-3-flash-preview"
        const val MODEL_PRO_IMAGE = "gemini-3-pro-image-preview"
    }

    object Fcm {
        const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
        const val BASE_URL = "https://fcm.googleapis.com/v1/projects"
        const val CONTENT_TYPE_JSON = "application/json"

        const val DEFAULT_CHANNEL_ID = "fcm_default_channel"
        const val DATE_FORMAT = "yyyy-MM-dd"
        const val TIME_FORMAT = "HH:mm"
        const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm"
    }

    object Ui {
        const val ALPHA_MEDIUM = 0.5f
        const val ALPHA_LOW = 0.3f
        const val FLIP_ROTATION = 180f
    }
}

