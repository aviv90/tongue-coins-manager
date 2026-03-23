package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.domain.model.Broadcast
import com.krumin.tonguecoinsmanager.domain.repository.BroadcastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BroadcastState(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val ctaText: String? = null,
    val ctaUrl: String? = null,
    val disabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
) {
    val isValid: Boolean
        get() {
            if (id.trim().isEmpty()) return false
            if (title.trim().isEmpty() && body.trim().isEmpty()) return false

            val hasCtaText = !ctaText.isNullOrBlank()
            val hasCtaUrl = !ctaUrl.isNullOrBlank()
            if (hasCtaText != hasCtaUrl) return false // Must be both or none

            if (!imageUrl.isNullOrBlank()) {
                if (!imageUrl.startsWith("https://") && !imageUrl.startsWith("http://")) return false
            }
            if (!ctaUrl.isNullOrBlank()) {
                if (!ctaUrl.startsWith("https://") && !ctaUrl.startsWith("http://")) return false
            }

            return true
        }
}

class BroadcastViewModel(
    private val repository: BroadcastRepository
) : ViewModel() {

    private val _id = MutableStateFlow("")
    private val _title = MutableStateFlow("")
    private val _body = MutableStateFlow("")
    private val _imageUrl = MutableStateFlow<String?>(null)
    private val _ctaText = MutableStateFlow<String?>(null)
    private val _ctaUrl = MutableStateFlow<String?>(null)
    private val _disabled = MutableStateFlow(false)

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _saveSuccess = MutableStateFlow(false)

    val state: StateFlow<BroadcastState> = combine(
        _id,
        _title,
        _body,
        _imageUrl,
        _ctaText,
        _ctaUrl,
        _disabled,
        _isLoading,
        _error,
        _saveSuccess
    ) { args ->
        BroadcastState(
            id = args[0] as String,
            title = args[1] as String,
            body = args[2] as String,
            imageUrl = args[3] as String?,
            ctaText = args[4] as String?,
            ctaUrl = args[5] as String?,
            disabled = args[6] as Boolean,
            isLoading = args[7] as Boolean,
            error = args[8] as String?,
            saveSuccess = args[9] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BroadcastState())

    init {
        loadBroadcast()
    }

    private fun loadBroadcast() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val broadcast = repository.getBroadcast()
                if (broadcast != null) {
                    _id.value = broadcast.id
                    _title.value = broadcast.title
                    _body.value = broadcast.body
                    _imageUrl.value = broadcast.imageUrl
                    _ctaText.value = broadcast.ctaText
                    _ctaUrl.value = broadcast.ctaUrl
                    _disabled.value = broadcast.disabled
                } else {
                    // Start with defaults if not exists
                    _id.value = "daily-${System.currentTimeMillis() / 1000}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load broadcast: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onIdChanged(value: String) {
        _id.value = value
    }

    fun onTitleChanged(value: String) {
        _title.value = value
    }

    fun onBodyChanged(value: String) {
        _body.value = value
    }

    fun onImageUrlChanged(value: String?) {
        _imageUrl.value = if (value.isNullOrBlank()) null else value
    }

    fun onCtaTextChanged(value: String?) {
        _ctaText.value = if (value.isNullOrBlank()) null else value
    }

    fun onCtaUrlChanged(value: String?) {
        _ctaUrl.value = if (value.isNullOrBlank()) null else value
    }

    fun onDisabledChanged(value: Boolean) {
        _disabled.value = value
    }

    fun onSave() {
        viewModelScope.launch {
            val currentState = state.value
            if (!currentState.isValid) {
                _error.value = "ביטול: נתונים לא תקינים. ודא שמזהה לא ריק, יש תוכן, ו-CTA תקין."
                return@launch
            }

            _isLoading.value = true
            _saveSuccess.value = false
            try {
                val broadcast = Broadcast(
                    id = currentState.id,
                    title = currentState.title,
                    body = currentState.body,
                    imageUrl = currentState.imageUrl,
                    ctaText = currentState.ctaText,
                    ctaUrl = currentState.ctaUrl,
                    disabled = currentState.disabled
                )
                repository.saveBroadcast(broadcast)
                _saveSuccess.value = true
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to save: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
}
