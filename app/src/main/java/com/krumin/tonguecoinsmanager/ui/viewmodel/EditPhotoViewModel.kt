package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.model.Platform
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.service.CategoryGenerator
import com.krumin.tonguecoinsmanager.domain.service.ImageEditor
import com.krumin.tonguecoinsmanager.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EditState(
    val isLoading: Boolean = false,
    val isGeneratingCategories: Boolean = false,
    val isEditingImage: Boolean = false,
    val photo: PhotoMetadata? = null,
    val error: UiText? = null,
    val isSuccess: Boolean = false,
    val generatedCategories: List<String>? = null,
    val pendingAiImage: File? = null, // Waiting for user approval
    val confirmedAiImage: File? = null, // User approved this image
    val validationErrors: Map<String, Int> = emptyMap() // Field name to error string resource ID
)

sealed interface EditAction {
    object LoadPhoto : EditAction
    data class SavePhoto(
        val title: String,
        val credit: String,
        val hint: String,
        val difficulty: Int,
        val categories: String,
        val supportedPlatforms: List<Platform>,
        val imageFile: File?
    ) : EditAction

    data class EditImage(val prompt: String) : EditAction
    data class GenerateCategories(val title: String) : EditAction
    object ClearGeneratedCategories : EditAction
    object ClearError : EditAction
    object ConfirmAiImage : EditAction
    object DiscardAiImage : EditAction
    object ClearConfirmedAiImage : EditAction
    object DeletePhoto : EditAction
}

class EditPhotoViewModel(
    private val repository: PhotoRepository,
    private val categoryGenerator: CategoryGenerator,
    private val imageEditor: ImageEditor,
    private val photoId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(EditState())
    val state = _state.asStateFlow()

    private var editJob: Job? = null
    private var categoryJob: Job? = null

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
            is EditAction.EditImage -> editImage(action.prompt)
            is EditAction.GenerateCategories -> generateCategories(action.title)
            is EditAction.ClearGeneratedCategories -> _state.update { it.copy(generatedCategories = null) }
            is EditAction.ClearError -> _state.update { it.copy(error = null) }
            is EditAction.ConfirmAiImage -> _state.update {
                it.copy(
                    confirmedAiImage = it.pendingAiImage,
                    pendingAiImage = null
                )
            }

            is EditAction.DiscardAiImage -> _state.update { it.copy(pendingAiImage = null) }
            is EditAction.ClearConfirmedAiImage -> _state.update { it.copy(confirmedAiImage = null) }
        }
    }

    private fun editImage(prompt: String) {
        if (prompt.isBlank()) return
        val currentPhoto = _state.value.photo ?: return

        editJob?.cancel()
        editJob = viewModelScope.launch {
            _state.update { it.copy(isEditingImage = true, error = null) }
            try {
                // Download current image to bytes
                val photoUrl = currentPhoto.imageUrl
                val response = withContext(Dispatchers.IO) {
                    java.net.URL(photoUrl).readBytes()
                }

                val editedBytes = imageEditor.editImage(response, prompt)

                // Save to temp file
                val tempFile = File.createTempFile(
                    com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.TEMP_PREFIX_AI,
                    com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig.Gcs.EXTENSION_JPG
                )
                tempFile.writeBytes(editedBytes)

                _state.update {
                    it.copy(
                        isEditingImage = false,
                        pendingAiImage = tempFile // Set as pending for review
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(
                        isEditingImage = false,
                        error = UiText.StringResource(R.string.error_ai_edit)
                    )
                }
            }
        }
    }

    private fun generateCategories(title: String) {
        if (title.isBlank()) return
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
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
                if (e is kotlinx.coroutines.CancellationException) throw e
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
                val pendingChanges = repository.pendingChanges.value

                // Check if there is a pending change for this ID
                val pendingChange = pendingChanges.find { it.id == id }

                val photo = when (pendingChange) {
                    is com.krumin.tonguecoinsmanager.domain.model.PendingChange.Add -> pendingChange.metadata
                    is com.krumin.tonguecoinsmanager.domain.model.PendingChange.Edit -> pendingChange.metadata
                    is com.krumin.tonguecoinsmanager.domain.model.PendingChange.Delete -> {
                        // If marked for deletion, we might still show it but maybe with a warning,
                        // or treat it as not found. For edit screen, showing the metadata is safer.
                        pendingChange.metadata
                    }

                    null -> photos.find { it.id == id }
                }

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
        val errors = mutableMapOf<String, Int>()
        if (action.title.isBlank()) errors["title"] = R.string.error_field_required
        if (action.categories.isBlank()) errors["categories"] = R.string.error_field_required
        if (action.difficulty < 1 || action.difficulty > 10) errors["difficulty"] =
            R.string.error_field_required

        // Image check: Confirmed AI > Selected > Existing
        val hasImage =
            _state.value.confirmedAiImage != null || action.imageFile != null || _state.value.photo?.imageUrl?.isNotBlank() == true
        if (!hasImage) errors["image"] = R.string.error_image_required

        if (errors.isNotEmpty()) {
            _state.update { it.copy(validationErrors = errors) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, validationErrors = emptyMap()) }
            try {
                val metadata = PhotoMetadata(
                    id = photoId ?: "",
                    imageUrl = _state.value.photo?.imageUrl ?: "",
                    title = action.title,
                    credit = action.credit,
                    hint = action.hint,
                    difficulty = action.difficulty,
                    categories = action.categories,
                    version = _state.value.photo?.version ?: 1,
                    supportedPlatforms = action.supportedPlatforms
                )

                // Priority: Confirmed AI image > User selected image > Existing image
                val imageToUpload = _state.value.confirmedAiImage ?: action.imageFile

                if (imageToUpload != null) {
                    repository.uploadPhoto(imageToUpload, metadata, commit = false)
                } else {
                    repository.updateMetadata(metadata, commit = false)
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
                repository.deletePhoto(id, commit = false)
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
