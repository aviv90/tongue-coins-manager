package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.PendingChange
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.util.UiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = false,
    val isCommitting: Boolean = false,
    val photos: List<PhotoMetadata> = emptyList(),
    val filteredPhotos: List<PhotoMetadata> = emptyList(),
    val pendingChanges: List<PendingChange> = emptyList(),
    val searchQuery: String = "",
    val error: UiText? = null,
    val isDownloading: Boolean = false,
    val downloadingPhotoId: String? = null,
    val downloadSuccess: UiText? = null,
    val commitSuccess: Boolean = false
)

sealed interface MainAction {
    object LoadPhotos : MainAction
    data class SearchQueryChanged(val query: String) : MainAction
    object ClearError : MainAction
    data class DownloadPhoto(val photo: PhotoMetadata) : MainAction
    object ClearDownloadStatus : MainAction
    object CommitChanges : MainAction
    object DiscardChanges : MainAction
    object ClearCommitStatus : MainAction
}

class MainViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private val _allPhotos = MutableStateFlow<List<PhotoMetadata>>(emptyList())
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            combine(_allPhotos, repository.pendingChanges) { photos, pending ->
                val merged = applyPendingChanges(photos, pending)
                merged to pending
            }.collect { (mergedPhotos, pending) ->
                _state.update { it.copy(pendingChanges = pending) }
                updateFilteredPhotos(_state.value.searchQuery, mergedPhotos)
            }
        }
        handleAction(MainAction.LoadPhotos)
    }

    private fun applyPendingChanges(
        photos: List<PhotoMetadata>,
        pending: List<PendingChange>
    ): List<PhotoMetadata> {
        val result = photos.toMutableList()
        pending.forEach { change ->
            when (change) {
                is PendingChange.Add -> {
                    result.add(change.metadata)
                }

                is PendingChange.Edit -> {
                    val index = result.indexOfFirst { it.id == change.id }
                    if (index != -1) {
                        result[index] = change.metadata
                    }
                }

                is PendingChange.Delete -> {
                    result.removeAll { it.id == change.id }
                }
            }
        }
        return result
    }

    fun handleAction(action: MainAction) {
        when (action) {
            is MainAction.LoadPhotos -> loadPhotos()
            is MainAction.SearchQueryChanged -> updateSearchQuery(action.query)
            is MainAction.ClearError -> _state.update { it.copy(error = null) }
            is MainAction.DownloadPhoto -> downloadPhoto(action.photo)
            is MainAction.ClearDownloadStatus -> _state.update { it.copy(downloadSuccess = null) }
            is MainAction.CommitChanges -> commitChanges()
            is MainAction.DiscardChanges -> discardChanges()
            is MainAction.ClearCommitStatus -> _state.update { it.copy(commitSuccess = false) }
        }
    }

    private fun loadPhotos() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            refreshPhotos()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun refreshPhotos() {
        try {
            val photos = repository.getPhotos()
            _allPhotos.value = photos
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _state.update {
                it.copy(
                    error = UiText.StringResource(R.string.error_loading_photos)
                )
            }
        }
    }

    private fun commitChanges() {
        viewModelScope.launch {
            _state.update { it.copy(isCommitting = true, error = null, commitSuccess = false) }
            try {
                repository.commitChanges()
                // Await a fresh load of photos before finishing the committing state
                refreshPhotos()
                _state.update { it.copy(isCommitting = false, commitSuccess = true) }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(
                        isCommitting = false,
                        error = UiText.StringResource(R.string.error_saving)
                    )
                }
            }
        }
    }

    private fun discardChanges() {
        viewModelScope.launch {
            repository.discardChanges()
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        updateFilteredPhotos(
            query,
            applyPendingChanges(_allPhotos.value, repository.pendingChanges.value)
        )
    }

    private fun updateFilteredPhotos(query: String, photos: List<PhotoMetadata>) {
        val filtered = if (query.isBlank()) {
            photos
        } else {
            photos.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.id.contains(query, ignoreCase = true)
            }
        }
        _state.update { it.copy(isLoading = false, filteredPhotos = filtered, photos = photos) }
    }

    private fun downloadPhoto(photo: PhotoMetadata) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDownloading = true,
                    downloadingPhotoId = photo.id,
                    error = null,
                    downloadSuccess = null
                )
            }
            try {
                repository.downloadPhoto(photo)
                _state.update {
                    it.copy(
                        isDownloading = false,
                        downloadingPhotoId = null,
                        downloadSuccess = UiText.StringResource(R.string.success_download)
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(
                        isDownloading = false,
                        downloadingPhotoId = null,
                        error = UiText.StringResource(R.string.error_download)
                    )
                }
            }
        }
    }
}
