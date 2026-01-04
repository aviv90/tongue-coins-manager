package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import com.krumin.tonguecoinsmanager.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class EditState(
    val isLoading: Boolean = false,
    val isGeneratingCategories: Boolean = false,
    val photo: PhotoMetadata? = null,
    val error: UiText? = null,
    val isSuccess: Boolean = false,
    val generatedCategories: List<String>? = null
)

sealed interface EditAction {
    object LoadPhoto : EditAction
    data class SavePhoto(
        val title: String,
        val credit: String,
        val hint: String,
        val difficulty: Int,
        val categories: String,
        val imageFile: File?
    ) : EditAction

    data class GenerateCategories(val title: String) : EditAction
    object ClearGeneratedCategories : EditAction
    object ClearError : EditAction
    object DeletePhoto : EditAction
}

class EditPhotoViewModel(
    private val repository: PhotoRepository,
    private val categoryGenerator: CategoryGenerator,
    private val photoId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(EditState())
    val state = _state.asStateFlow()

    init {
        if (photoId != null) {
            handleAction(EditAction.LoadPhoto)
        }
    }

    fun handleAction(action: EditAction) {
        when (action) {
            is EditAction.LoadPhoto -> loadPhoto()
            is EditAction.SavePhoto -> save(action)
            is EditAction.DeletePhoto -> delete()
            is EditAction.GenerateCategories -> generateCategories(action.title)
            is EditAction.ClearGeneratedCategories -> _state.update { it.copy(generatedCategories = null) }
            is EditAction.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun generateCategories(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isGeneratingCategories = true,
                    error = null,
                    generatedCategories = null
                )
            }
            try {
                val contextPhotos = repository.getPhotos()
                    .filter { it.id != photoId && it.title != title }
                val categories = categoryGenerator.generateCategories(title, contextPhotos)
                if (categories.isEmpty()) {
                    _state.update {
                        it.copy(
                            isGeneratingCategories = false,
                            error = UiText.StringResource(R.string.error_ai_generation)
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isGeneratingCategories = false,
                            generatedCategories = categories
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isGeneratingCategories = false) }
            }
        }
    }

    private fun loadPhoto() {
        val id = photoId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val photos = repository.getPhotos()
                val photo = photos.find { it.id == id }
                _state.update { it.copy(isLoading = false, photo = photo) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(R.string.error_loading_photo)
                    )
                }
            }
        }
    }

    private fun save(action: EditAction.SavePhoto) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val metadata = PhotoMetadata(
                    id = photoId ?: "",
                    imageUrl = _state.value.photo?.imageUrl ?: "",
                    title = action.title,
                    credit = action.credit,
                    hint = action.hint,
                    difficulty = action.difficulty,
                    categories = action.categories,
                    version = _state.value.photo?.version ?: 1
                )

                if (action.imageFile != null) {
                    repository.uploadPhoto(action.imageFile, metadata)
                } else {
                    repository.updateMetadata(metadata)
                }
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(R.string.error_saving)
                    )
                }
            }
        }
    }

    private fun delete() {
        val id = photoId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                repository.deletePhoto(id)
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(R.string.error_deleting)
                    )
                }
            }
        }
    }
}
