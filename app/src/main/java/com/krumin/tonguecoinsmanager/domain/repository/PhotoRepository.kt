package com.krumin.tonguecoinsmanager.domain.repository

import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import kotlinx.coroutines.flow.Flow
import java.io.File

interface PhotoRepository {
    suspend fun getPhotos(): List<PhotoMetadata>
    suspend fun uploadPhoto(imageFile: File, metadata: PhotoMetadata)
    suspend fun updateMetadata(metadata: PhotoMetadata)
    suspend fun deletePhoto(id: String)
}
