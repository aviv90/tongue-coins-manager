package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
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
import kotlinx.serialization.encodeToString
import java.io.File

class GcsPhotoRepository(
    private val context: Context,
    private val bucketName: String,
    private val jsonFileName: String = "content.json"
) : PhotoRepository {

    private val storage: Storage by lazy {
        // NOTE: In a real app, you would load credentials from a secure place.
        // For development, we might use a service account key in assets or raw.
        // This is a placeholder for the actual authentication logic.
        try {
            val serviceAccountStream = context.assets.open("gcp-key.json")
            val credentials = GoogleCredentials.fromStream(serviceAccountStream)
            StorageOptions.newBuilder().setCredentials(credentials).build().service
        } catch (e: Exception) {
            // Fallback to default credentials if not found
            StorageOptions.getDefaultInstance().service
        }
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
    }

    override suspend fun getPhotos(): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val blob = storage.get(BlobId.of(bucketName, jsonFileName))
        if (blob == null || blob.getSize() == 0L) return@withContext emptyList()
        
        val content = String(blob.getContent())
        json.decodeFromString<List<PhotoMetadata>>(content)
    }

    override suspend fun uploadPhoto(imageFile: File, metadata: PhotoMetadata) = withContext(Dispatchers.IO) {
        // 1. Upload image
        val blobId = BlobId.of(bucketName, "${metadata.id}.jpeg")
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/jpeg").build()
        storage.create(blobInfo, imageFile.readBytes())

        // 2. Update JSON
        val currentPhotos = getPhotos().toMutableList()
        val index = currentPhotos.indexOfFirst { it.id == metadata.id }
        if (index != -1) {
            currentPhotos[index] = metadata
        } else {
            currentPhotos.add(metadata)
        }
        savePhotosJson(currentPhotos)
    }

    override suspend fun updateMetadata(metadata: PhotoMetadata) = withContext(Dispatchers.IO) {
        val currentPhotos = getPhotos().toMutableList()
        val index = currentPhotos.indexOfFirst { it.id == metadata.id }
        if (index != -1) {
            currentPhotos[index] = metadata
            savePhotosJson(currentPhotos)
        }
    }

    override suspend fun deletePhoto(id: String) = withContext(Dispatchers.IO) {
        // 1. Delete image
        storage.delete(BlobId.of(bucketName, "$id.jpeg"))

        // 2. Update JSON
        val currentPhotos = getPhotos().toMutableList()
        currentPhotos.removeAll { it.id == id }
        savePhotosJson(currentPhotos)
    }

    private fun savePhotosJson(photos: List<PhotoMetadata>) {
        val jsonContent = json.encodeToString(photos)
        val blobId = BlobId.of(bucketName, jsonFileName)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
        storage.create(blobInfo, jsonContent.toByteArray())
    }
}
