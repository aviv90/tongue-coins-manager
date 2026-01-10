package com.krumin.tonguecoinsmanager.domain.repository

import com.krumin.tonguecoinsmanager.domain.model.PendingChange
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface PhotoRepository {
    val pendingChanges: StateFlow<List<PendingChange>>

    suspend fun getPhotos(): List<PhotoMetadata>

    suspend fun uploadPhoto(imageFile: File, metadata: PhotoMetadata, commit: Boolean = true)
    suspend fun updateMetadata(metadata: PhotoMetadata, commit: Boolean = true)
    suspend fun deletePhoto(id: String, commit: Boolean = true)

    suspend fun commitChanges()
    suspend fun discardChanges()

    suspend fun downloadPhoto(photo: PhotoMetadata)
}
