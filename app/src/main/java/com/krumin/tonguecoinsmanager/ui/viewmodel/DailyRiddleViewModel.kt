package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.domain.model.DailyRiddle
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.GetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.ResetDailyRiddleUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle.SetDailyRiddleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyRiddleState(
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val currentRiddle: DailyRiddle? = null,
    val availableRiddles: List<PhotoMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class DailyRiddleViewModel(
    private val getDailyRiddleUseCase: GetDailyRiddleUseCase,
    private val setDailyRiddleUseCase: SetDailyRiddleUseCase,
    private val resetDailyRiddleUseCase: ResetDailyRiddleUseCase,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _selectedDate =
        MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Combine flows to create the UI state
    val state: StateFlow<DailyRiddleState> = combine(
        _selectedDate,
        _selectedDate.flatMapLatest { date -> getDailyRiddleUseCase(date) },
        _isLoading,
        _error
    ) { date, riddle, isLoading, error ->
        DailyRiddleState(
            selectedDate = date,
            currentRiddle = riddle,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyRiddleState())

    // Separate flow for available riddles to avoid re-fetching on date change if not needed
    // In a real app we might want to paginate or filter this, but for now we load all.
    private val _availableRiddles = MutableStateFlow<List<PhotoMetadata>>(emptyList())
    val availableRiddles: StateFlow<List<PhotoMetadata>> = _availableRiddles.asStateFlow()

    init {
        loadAvailableRiddles()
    }

    private fun loadAvailableRiddles() {
        viewModelScope.launch {
            try {
                _availableRiddles.value = photoRepository.getPhotos()
            } catch (e: Exception) {
                _error.update { "Failed to load riddles: ${e.message}" }
            }
        }
    }

    fun onDateSelected(date: String) {
        _selectedDate.value = date
    }

    fun onSetRiddle(contentItemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                setDailyRiddleUseCase(_selectedDate.value, contentItemId)
                _error.value = null // Clear error on success
            } catch (e: Exception) {
                _error.value = "Failed to set riddle: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onResetRiddle() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                resetDailyRiddleUseCase(_selectedDate.value)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to reset riddle: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
