package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainState(
    val isLoading: Boolean = false,
    val photos: List<PhotoMetadata> = emptyList(),
    val filteredPhotos: List<PhotoMetadata> = emptyList(),
    val searchQuery: String = "",
    val error: UiText? = null
)

sealed interface MainAction {
    object LoadPhotos : MainAction
    data class SearchQueryChanged(val query: String) : MainAction
    object ClearError : MainAction
}

class MainViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state = _state.asStateFlow()

    private val _allPhotos = MutableStateFlow<List<PhotoMetadata>>(emptyList())

    init {
        handleAction(MainAction.LoadPhotos)
    }

    fun handleAction(action: MainAction) {
        when (action) {
            is MainAction.LoadPhotos -> loadPhotos()
            is MainAction.SearchQueryChanged -> updateSearchQuery(action.query)
            is MainAction.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val photos = repository.getPhotos()
                _allPhotos.value = photos
                updateFilteredPhotos(_state.value.searchQuery, photos)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(R.string.error_loading_photos)
                    )
                }
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        updateFilteredPhotos(query, _allPhotos.value)
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
}
