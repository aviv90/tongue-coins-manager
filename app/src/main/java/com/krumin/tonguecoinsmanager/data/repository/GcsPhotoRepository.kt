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
import com.krumin.tonguecoinsmanager.data.local.ChangeType
import com.krumin.tonguecoinsmanager.data.local.PendingChangeDao
import com.krumin.tonguecoinsmanager.data.local.PendingChangeEntity
import com.krumin.tonguecoinsmanager.domain.model.PendingChange
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class GcsPhotoRepository(
    private val context: Context,
    private val pendingChangeDao: PendingChangeDao,
    private val privateBucketName: String,
    private val publicBucketName: String,
    private val jsonFileName: String = DEFAULT_JSON_FILE,
    private val keyFileName: String = DEFAULT_KEY_FILE
) : PhotoRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingImagesDir = File(context.filesDir, "pending_images")

    init {
        if (!pendingImagesDir.exists()) {
            pendingImagesDir.mkdirs()
        }
    }

    override val pendingChanges: StateFlow<List<PendingChange>> = pendingChangeDao.getAll()
        .map { entities -> entities.mapNotNull { it.toDomainModel() } }
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

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

    private fun PendingChangeEntity.toDomainModel(): PendingChange? {
        val metadata = try {
            json.decodeFromString<PhotoMetadata>(metadataJson)
        } catch (e: Exception) {
            return null
        }
        return when (type) {
            ChangeType.ADD -> {
                val file = imageFilePath?.let { File(it) }
                if (file?.exists() == true) {
                    PendingChange.Add(id, metadata, file)
                } else null
            }

            ChangeType.EDIT -> PendingChange.Edit(id, metadata, imageFilePath?.let { File(it) })
            ChangeType.DELETE -> PendingChange.Delete(id, metadata)
        }
    }

    private fun PendingChange.toEntity(): PendingChangeEntity {
        return when (this) {
            is PendingChange.Add -> PendingChangeEntity(
                id = id,
                type = ChangeType.ADD,
                metadataJson = json.encodeToString(metadata),
                imageFilePath = imageFile.absolutePath
            )

            is PendingChange.Edit -> PendingChangeEntity(
                id = id,
                type = ChangeType.EDIT,
                metadataJson = json.encodeToString(metadata),
                imageFilePath = newImageFile?.absolutePath
            )

            is PendingChange.Delete -> PendingChangeEntity(
                id = id,
                type = ChangeType.DELETE,
                metadataJson = json.encodeToString(metadata),
                imageFilePath = null
            )
        }
    }

    override suspend fun getPhotos(): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val blob = storage.get(BlobId.of(privateBucketName, jsonFileName))
        if (blob == null || blob.size == 0L) return@withContext emptyList()

        val content = String(blob.getContent())
        json.decodeFromString<List<PhotoMetadata>>(content)
    }

    private fun generateNextId(
        currentPhotos: List<PhotoMetadata>,
        pendingChanges: List<PendingChangeEntity>
    ): String {
        val serverMaxId = currentPhotos
            .mapNotNull { it.id.removePrefix(ID_PREFIX).toIntOrNull() }
            .maxOrNull() ?: 0

        val pendingMaxId = pendingChanges
            .mapNotNull { it.id.removePrefix(ID_PREFIX).toIntOrNull() }
            .maxOrNull() ?: 0

        val maxId = maxOf(serverMaxId, pendingMaxId)
        return "$ID_PREFIX${maxId + 1}"
    }

    override suspend fun uploadPhoto(imageFile: File, metadata: PhotoMetadata, commit: Boolean) {
        withContext(Dispatchers.IO) {
            val currentPhotos = getPhotos()
            val pendingChanges = pendingChangeDao.getAllSync()

            // Generate ID considering BOTH server photos AND pending changes to avoid collision
            val isNew = currentPhotos.none { it.id == metadata.id } || metadata.id.isEmpty()
            val finalId = if (isNew) generateNextId(currentPhotos, pendingChanges) else metadata.id

            // Check for existing pending change FIRST to use its version as base
            val existingPending = pendingChanges.find { it.id == finalId }
            val existingPendingMetadata = existingPending?.let {
                try {
                    json.decodeFromString<PhotoMetadata>(it.metadataJson)
                } catch (e: Exception) {
                    null
                }
            }

            // For updates: prefer pending metadata, then server metadata
            val existing = existingPendingMetadata ?: currentPhotos.find { it.id == finalId }
            val finalVersion = if (isNew) 1 else (existing?.version ?: 0) + 1
            val finalImageUrl = "$GCS_BASE_URL/$publicBucketName/$finalId$IMAGE_EXT?v=$finalVersion"

            // Calculate aspect ratio
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            val aspectRatio =
                if (options.outHeight > 0) options.outWidth.toFloat() / options.outHeight else 1.0f

            val finalMetadata = (existing ?: metadata).copy(
                id = finalId,
                imageUrl = finalImageUrl,
                title = metadata.title,
                credit = metadata.credit,
                hint = metadata.hint,
                difficulty = metadata.difficulty,
                categories = metadata.categories,
                version = finalVersion,
                aspectRatio = aspectRatio,
                supportedPlatforms = metadata.supportedPlatforms
            ).trimmed()

            if (commit) {
                // 1. Upload image
                val blobId = BlobId.of(publicBucketName, "$finalId$IMAGE_EXT")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType(CONTENT_TYPE_JPEG).build()
                storage.create(blobInfo, imageFile.readBytes())

                // 2. Update JSON
                val updatedList = currentPhotos.toMutableList()
                val existingIndex = updatedList.indexOfFirst { it.id == finalId }
                if (existingIndex != -1) {
                    updatedList[existingIndex] = finalMetadata
                } else {
                    updatedList.add(finalMetadata)
                }
                savePhotosJson(updatedList)
            } else {
                // Copy image to persistent internal storage
                val persistentFile =
                    File(pendingImagesDir, "${finalId}_${System.currentTimeMillis()}.jpg")
                imageFile.copyTo(persistentFile, overwrite = true)

                // Check for existing pending change
                val existingEntity = pendingChangeDao.getAllSync().find { it.id == finalId }

                val change = if (isNew || existingEntity?.type == ChangeType.ADD) {
                    PendingChange.Add(finalId, finalMetadata, persistentFile)
                } else {
                    PendingChange.Edit(finalId, finalMetadata, persistentFile)
                }
                pendingChangeDao.upsert(change.toEntity())

                // If there was an old file, let's clean it up (optional but good)
                existingEntity?.imageFilePath?.let { oldPath ->
                    if (oldPath != persistentFile.absolutePath && oldPath.startsWith(
                            pendingImagesDir.absolutePath
                        )
                    ) {
                        File(oldPath).delete()
                    }
                }
            }
        }
    }

    override suspend fun updateMetadata(metadata: PhotoMetadata, commit: Boolean) {
        withContext(Dispatchers.IO) {
            val currentPhotos = getPhotos()
            val existingEntity = pendingChangeDao.getAllSync().find { it.id == metadata.id }

            // Use pending change metadata as base if exists, otherwise use server data
            val baseMetadata = if (existingEntity != null) {
                try {
                    json.decodeFromString<PhotoMetadata>(existingEntity.metadataJson)
                } catch (e: Exception) {
                    currentPhotos.find { it.id == metadata.id }
                }
            } else {
                currentPhotos.find { it.id == metadata.id }
            } ?: return@withContext

            val updatedMetadata = baseMetadata.copy(
                title = metadata.title,
                credit = metadata.credit,
                hint = metadata.hint,
                difficulty = metadata.difficulty,
                categories = metadata.categories,
                imageUrl = "${baseMetadata.imageUrl.substringBefore("?v=")}?v=${baseMetadata.version + 1}",
                version = baseMetadata.version + 1,
                supportedPlatforms = metadata.supportedPlatforms
            ).trimmed()

            if (commit) {
                val updatedList = currentPhotos.toMutableList()
                val index = updatedList.indexOfFirst { it.id == metadata.id }
                if (index != -1) {
                    updatedList[index] = updatedMetadata
                    savePhotosJson(updatedList)
                }
            } else {
                val change = when (existingEntity?.type) {
                    ChangeType.ADD -> {
                        // Preserve the Add type and original image file
                        val originalFile = existingEntity.imageFilePath?.let { File(it) }
                        if (originalFile?.exists() == true) {
                            PendingChange.Add(metadata.id, updatedMetadata, originalFile)
                        } else {
                            PendingChange.Edit(metadata.id, updatedMetadata)
                        }
                    }

                    ChangeType.EDIT -> {
                        // Preserve any existing image file
                        PendingChange.Edit(
                            metadata.id,
                            updatedMetadata,
                            existingEntity.imageFilePath?.let { File(it) })
                    }

                    else -> PendingChange.Edit(metadata.id, updatedMetadata)
                }
                pendingChangeDao.upsert(change.toEntity())
            }
        }
    }

    override suspend fun deletePhoto(id: String, commit: Boolean) {
        withContext(Dispatchers.IO) {
            val currentPhotos = getPhotos()
            if (commit) {
                storage.delete(BlobId.of(publicBucketName, "$id$IMAGE_EXT"))
                val updatedPhotos = currentPhotos.toMutableList()
                updatedPhotos.removeAll { it.id == id }
                savePhotosJson(updatedPhotos)
            } else {
                val existingEntity = pendingChangeDao.getAllSync().find { it.id == id }

                if (existingEntity?.type == ChangeType.ADD) {
                    // Clean up internal image file
                    existingEntity.imageFilePath?.let { File(it).delete() }
                    // If it was added in this batch, just remove the add change
                    pendingChangeDao.delete(id)
                } else {
                    val existingMetadata = currentPhotos.find { it.id == id }
                    if (existingMetadata != null) {
                        pendingChangeDao.upsert(
                            PendingChange.Delete(id, existingMetadata).toEntity()
                        )
                    }
                }
            }
        }
    }

    override suspend fun commitChanges() {
        withContext(Dispatchers.IO) {
            val entities = pendingChangeDao.getAllSync()
            if (entities.isEmpty()) return@withContext

            val changes = entities.mapNotNull { it.toDomainModel() }
            val currentPhotos = getPhotos().toMutableList()

            changes.forEach { change ->
                when (change) {
                    is PendingChange.Add -> {
                        // Verify file exists
                        if (!change.imageFile.exists()) {
                            throw IllegalStateException("Missing image file for adding photo ${change.id}")
                        }
                        // Upload image
                        val blobId = BlobId.of(publicBucketName, "${change.id}$IMAGE_EXT")
                        val blobInfo =
                            BlobInfo.newBuilder(blobId).setContentType(CONTENT_TYPE_JPEG).build()
                        storage.create(blobInfo, change.imageFile.readBytes())

                        // Add to list
                        currentPhotos.add(change.metadata)
                    }

                    is PendingChange.Edit -> {
                        // Upload new image if present
                        change.newImageFile?.let { imageFile ->
                            if (!imageFile.exists()) {
                                throw IllegalStateException("Missing new image file for editing photo ${change.id}")
                            }
                            val blobId = BlobId.of(publicBucketName, "${change.id}$IMAGE_EXT")
                            val blobInfo =
                                BlobInfo.newBuilder(blobId).setContentType(CONTENT_TYPE_JPEG)
                                    .build()
                            storage.create(blobInfo, imageFile.readBytes())
                        }

                        // Update in list
                        val index = currentPhotos.indexOfFirst { it.id == change.id }
                        if (index != -1) {
                            currentPhotos[index] = change.metadata
                        }
                    }

                    is PendingChange.Delete -> {
                        // Delete image
                        storage.delete(BlobId.of(publicBucketName, "${change.id}$IMAGE_EXT"))

                        // Remove from list
                        currentPhotos.removeAll { it.id == change.id }
                    }
                }
            }

            savePhotosJson(currentPhotos)

            // Success! Clean up all pending internal files
            pendingImagesDir.listFiles()?.forEach { it.delete() }
            // Only clear from database after successful commit
            pendingChangeDao.deleteAll()
        }
    }

    override suspend fun discardChanges() {
        pendingImagesDir.listFiles()?.forEach { it.delete() }
        pendingChangeDao.deleteAll()
    }

    override suspend fun downloadPhoto(photo: PhotoMetadata) {
        withContext(Dispatchers.IO) {
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
    }

    private suspend fun savePhotosJson(photos: List<PhotoMetadata>) = withContext(Dispatchers.IO) {
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

    override suspend fun cancelDeletion(id: String) {
        withContext(Dispatchers.IO) {
            val pending = pendingChangeDao.getAllSync().find { it.id == id }
            if (pending?.type == ChangeType.DELETE) {
                pendingChangeDao.delete(id)
            }
        }
    }
}
