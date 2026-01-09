package com.krumin.tonguecoinsmanager.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class GcsPhotoRepository(
    private val context: Context,
    private val privateBucketName: String,
    private val publicBucketName: String,
    private val jsonFileName: String = DEFAULT_JSON_FILE,
    private val keyFileName: String = DEFAULT_KEY_FILE
) : PhotoRepository {

    private val storage: Storage by lazy {
        try {
            val serviceAccountStream = context.assets.open(keyFileName)
            val credentials = GoogleCredentials.fromStream(serviceAccountStream)
            StorageOptions.newBuilder().setCredentials(credentials).build().service
        } catch (e: Exception) {
            StorageOptions.getDefaultInstance().service
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    override suspend fun getPhotos(): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val blob = storage.get(BlobId.of(privateBucketName, jsonFileName))
        if (blob == null || blob.size == 0L) return@withContext emptyList()

        val content = String(blob.getContent())
        json.decodeFromString<List<PhotoMetadata>>(content)
    }

    private fun generateNextId(currentPhotos: List<PhotoMetadata>): String {
        val maxId = currentPhotos
            .mapNotNull { it.id.removePrefix(ID_PREFIX).toIntOrNull() }
            .maxOrNull() ?: 0
        return "$ID_PREFIX${maxId + 1}"
    }

    override suspend fun uploadPhoto(imageFile: File, metadata: PhotoMetadata) =
        withContext(Dispatchers.IO) {
            val currentPhotos = getPhotos().toMutableList()
            val isNew = currentPhotos.none { it.id == metadata.id } || metadata.id.isEmpty()

            val finalId = if (isNew) generateNextId(currentPhotos) else metadata.id
            val existingIndex = currentPhotos.indexOfFirst { it.id == finalId }
            val existing = if (existingIndex != -1) currentPhotos[existingIndex] else null
            val finalVersion = if (isNew) 1 else (existing?.version ?: 0) + 1
            val finalImageUrl = "$GCS_BASE_URL/$publicBucketName/$finalId$IMAGE_EXT?v=$finalVersion"

            // Calculate aspect ratio
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            val aspectRatio =
                if (options.outHeight > 0) options.outWidth.toFloat() / options.outHeight else 1.0f

            // For updates: merge with existing data to preserve all fields
            val finalMetadata = if (existing != null) {
                existing.copy(
                    id = finalId,
                    imageUrl = finalImageUrl,
                    title = metadata.title,
                    credit = metadata.credit,
                    hint = metadata.hint,
                    difficulty = metadata.difficulty,
                    categories = metadata.categories,
                    version = finalVersion,
                    aspectRatio = aspectRatio
                )
            } else {
                metadata.copy(
                    id = finalId,
                    imageUrl = finalImageUrl,
                    version = finalVersion,
                    aspectRatio = aspectRatio
                )
            }

            // 1. Upload image
            val blobId = BlobId.of(publicBucketName, "$finalId$IMAGE_EXT")
            val blobInfo = BlobInfo.newBuilder(blobId).setContentType(CONTENT_TYPE_JPEG).build()
            storage.create(blobInfo, imageFile.readBytes())

            // 2. Update JSON
            if (existingIndex != -1) {
                currentPhotos[existingIndex] = finalMetadata.trimmed()
            } else {
                currentPhotos.add(finalMetadata.trimmed())
            }
            savePhotosJson(currentPhotos)
        }

    override suspend fun updateMetadata(metadata: PhotoMetadata) = withContext(Dispatchers.IO) {
        val currentPhotos = getPhotos().toMutableList()
        val index = currentPhotos.indexOfFirst { it.id == metadata.id }
        if (index != -1) {
            val existing = currentPhotos[index]
            // Merge: Use incoming values but preserve any fields that might be missing
            val updatedMetadata = existing.copy(
                title = metadata.title,
                credit = metadata.credit,
                hint = metadata.hint,
                difficulty = metadata.difficulty,
                categories = metadata.categories,
                imageUrl = "${existing.imageUrl.substringBefore("?v=")}?v=${existing.version + 1}",
                version = existing.version + 1
            )
            currentPhotos[index] = updatedMetadata.trimmed()
            savePhotosJson(currentPhotos)
        }
    }

    override suspend fun deletePhoto(id: String) = withContext(Dispatchers.IO) {
        storage.delete(BlobId.of(publicBucketName, "$id$IMAGE_EXT"))
        val currentPhotos = getPhotos().toMutableList()
        currentPhotos.removeAll { it.id == id }
        savePhotosJson(currentPhotos)
    }

    override suspend fun downloadPhoto(photo: PhotoMetadata): Unit = withContext(Dispatchers.IO) {
        val blob = storage.get(BlobId.of(publicBucketName, "${photo.id}$IMAGE_EXT"))
        if (blob == null) return@withContext

        val bytes = blob.getContent()
        val displayName = "${photo.title.replace(" ", "_")}_${photo.id}$IMAGE_EXT"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, CONTENT_TYPE_JPEG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.DOWNLOAD_FOLDER
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(bytes)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        }
    }

    private fun savePhotosJson(photos: List<PhotoMetadata>) {
        val jsonContent = json.encodeToString(photos)
        val blobId = BlobId.of(privateBucketName, jsonFileName)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(CONTENT_TYPE_JSON).build()
        storage.create(blobInfo, jsonContent.toByteArray())
    }

    companion object {
        private const val DEFAULT_JSON_FILE =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.DEFAULT_JSON_FILE
        private const val DEFAULT_KEY_FILE =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.DEFAULT_KEY_FILE
        private const val GCS_BASE_URL =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.BASE_URL
        private const val ID_PREFIX =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.ID_PREFIX
        private const val IMAGE_EXT =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.IMAGE_EXT
        private const val CONTENT_TYPE_JPEG =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.CONTENT_TYPE_JPEG
        private const val CONTENT_TYPE_JSON =
            com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.CONTENT_TYPE_JSON
    }
}
