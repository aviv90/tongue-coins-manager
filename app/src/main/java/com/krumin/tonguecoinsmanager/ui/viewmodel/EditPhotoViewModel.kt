package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed class EditUiState {
    object Idle : EditUiState()
    object Loading : EditUiState()
    object Success : EditUiState()
    data class Error(val message: String) : EditUiState()
}

class EditPhotoViewModel(
    private val repository: PhotoRepository,
    private val photoId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditUiState>(EditUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _currentPhoto = MutableStateFlow<PhotoMetadata?>(null)
    val currentPhoto = _currentPhoto.asStateFlow()

    init {
        if (photoId != null) {
            loadPhoto(photoId)
        }
    }

    private fun loadPhoto(id: String) {
        viewModelScope.launch {
            try {
                val photos = repository.getPhotos()
                _currentPhoto.value = photos.find { it.id == id }
            } catch (e: Exception) {
                _uiState.value = EditUiState.Error("Failed to load photo")
            }
        }
    }

    fun savePhoto(
        title: String,
        credit: String,
        hint: String,
        difficulty: Int,
        categories: String,
        imageFile: File?
    ) {
        viewModelScope.launch {
            _uiState.value = EditUiState.Loading
            try {
                val metadata = PhotoMetadata(
                    id = _currentPhoto.value?.id ?: UUID.randomUUID().toString(),
                    imageUrl = _currentPhoto.value?.imageUrl ?: "", // Will be updated by repository if needed
                    title = title,
                    credit = credit,
                    hint = hint,
                    difficulty = difficulty,
                    categories = categories,
                    version = (_currentPhoto.value?.version ?: 0) + 1
                )

                if (imageFile != null) {
                    repository.uploadPhoto(imageFile, metadata)
                } else {
                    repository.updateMetadata(metadata)
                }
                _uiState.value = EditUiState.Success
            } catch (e: Exception) {
                _uiState.value = EditUiState.Error(e.message ?: "Failed to save")
            }
        }
    }
}
