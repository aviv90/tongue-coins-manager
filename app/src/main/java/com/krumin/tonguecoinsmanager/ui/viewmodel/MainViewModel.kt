package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val photos: List<PhotoMetadata>) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

class MainViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                val photos = repository.getPhotos()
                _uiState.value = MainUiState.Success(photos)
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deletePhoto(id: String) {
        viewModelScope.launch {
            try {
                repository.deletePhoto(id)
                loadPhotos()
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(e.message ?: "Failed to delete")
            }
        }
    }
}
