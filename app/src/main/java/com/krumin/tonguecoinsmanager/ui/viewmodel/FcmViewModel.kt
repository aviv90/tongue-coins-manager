package com.krumin.tonguecoinsmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.FcmPriority
import com.krumin.tonguecoinsmanager.domain.model.NotificationTarget
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FcmState(
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val dataPayload: Map<String, String> = emptyMap(),
    val target: NotificationTarget = NotificationTarget.Topic("all"),
    val selectedPlatforms: Set<com.krumin.tonguecoinsmanager.domain.model.Platform> = emptySet(),
    val testDevices: List<TestDevice> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val success: UiText? = null,
    val isDryRun: Boolean = false,

    // AI Generation
    val ideaPrompt: String = "",
    val isGenerating: Boolean = false,

    // Advanced Fields
    val priority: FcmPriority = FcmPriority.HIGH,
    val androidChannelId: String? = "general",
    val soundEnabled: Boolean = true,
    val badgeCount: Int? = null,

    // Scheduling
    val isScheduled: Boolean = false,
    val scheduledDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val scheduledTime: String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
)

sealed interface FcmAction {
    data class UpdateTitle(val title: String) : FcmAction
    data class UpdateBody(val body: String) : FcmAction
    data class UpdateImageUrl(val url: String?) : FcmAction
    data class AddDataPair(val key: String, val value: String) : FcmAction
    data class RemoveDataPair(val key: String) : FcmAction
    data class UpdateTarget(val target: NotificationTarget) : FcmAction
    data class UpdateSelectedPlatforms(val platforms: Set<com.krumin.tonguecoinsmanager.domain.model.Platform>) :
        FcmAction

    data class SendNotification(val overrideTarget: NotificationTarget? = null) : FcmAction
    data class UpdateDryRun(val enabled: Boolean) : FcmAction

    // AI Generation
    data class UpdateIdeaPrompt(val prompt: String) : FcmAction
    object GenerateContent : FcmAction

    // Advanced Fields
    data class UpdatePriority(val priority: FcmPriority) : FcmAction
    data class UpdateChannelId(val channelId: String?) : FcmAction
    data class UpdateSoundEnabled(val enabled: Boolean) : FcmAction
    data class UpdateBadgeCount(val count: Int?) : FcmAction

    // Scheduling
    data class UpdateIsScheduled(val enabled: Boolean) : FcmAction
    data class UpdateScheduledDate(val date: String) : FcmAction
    data class UpdateScheduledTime(val time: String) : FcmAction

    // Test Devices
    data class AddTestDevice(val name: String, val token: String) : FcmAction
    data class RemoveTestDevice(val device: TestDevice) : FcmAction

    object ClearStatus : FcmAction
    object ClearForm : FcmAction
}

class FcmViewModel(
    private val sendFcmUseCase: com.krumin.tonguecoinsmanager.domain.usecase.fcm.SendFcmNotificationUseCase,
    private val scheduleFcmUseCase: com.krumin.tonguecoinsmanager.domain.usecase.fcm.ScheduleFcmNotificationUseCase,
    private val generateFcmUseCase: com.krumin.tonguecoinsmanager.domain.usecase.fcm.GenerateFcmNotificationUseCase,
    private val manageTestDevicesUseCase: com.krumin.tonguecoinsmanager.domain.usecase.fcm.ManageTestDevicesUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "FcmViewModel"
    }

    private val _state = MutableStateFlow(FcmState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            manageTestDevicesUseCase.getDevices().collect { devices ->
                _state.update { it.copy(testDevices = devices) }
            }
        }
    }

    fun handleAction(action: FcmAction) {
        when (action) {
            is FcmAction.UpdateTitle -> _state.update { it.copy(title = action.title) }
            is FcmAction.UpdateBody -> _state.update { it.copy(body = action.body) }
            is FcmAction.UpdateImageUrl -> _state.update { it.copy(imageUrl = action.url) }
            is FcmAction.AddDataPair -> _state.update {
                it.copy(dataPayload = it.dataPayload + (action.key to action.value))
            }

            is FcmAction.RemoveDataPair -> _state.update {
                it.copy(dataPayload = it.dataPayload - action.key)
            }

            is FcmAction.UpdateTarget -> _state.update { it.copy(target = action.target) }
            is FcmAction.UpdateSelectedPlatforms -> _state.update { it.copy(selectedPlatforms = action.platforms) }
            is FcmAction.UpdateDryRun -> _state.update { it.copy(isDryRun = action.enabled) }
            is FcmAction.UpdateIdeaPrompt -> _state.update { it.copy(ideaPrompt = action.prompt) }
            FcmAction.GenerateContent -> generateContent()
            is FcmAction.UpdatePriority -> _state.update { it.copy(priority = action.priority) }
            is FcmAction.UpdateChannelId -> _state.update { it.copy(androidChannelId = action.channelId) }
            is FcmAction.UpdateSoundEnabled -> _state.update { it.copy(soundEnabled = action.enabled) }
            is FcmAction.UpdateBadgeCount -> _state.update { it.copy(badgeCount = action.count) }
            is FcmAction.UpdateIsScheduled -> _state.update { it.copy(isScheduled = action.enabled) }
            is FcmAction.UpdateScheduledDate -> _state.update { it.copy(scheduledDate = action.date) }
            is FcmAction.UpdateScheduledTime -> _state.update { it.copy(scheduledTime = action.time) }
            is FcmAction.SendNotification -> sendNotification(action.overrideTarget)
            is FcmAction.AddTestDevice -> addTestDevice(action.name, action.token)
            is FcmAction.RemoveTestDevice -> removeTestDevice(action.device)
            FcmAction.ClearStatus -> _state.update { it.copy(error = null, success = null) }
            FcmAction.ClearForm -> _state.update { FcmState(testDevices = it.testDevices) }
        }
    }

    private fun generateContent() {
        viewModelScope.launch {
            val prompt = _state.value.ideaPrompt
            if (prompt.isBlank()) return@launch

            _state.update { it.copy(isGenerating = true, error = null) }
            val result = generateFcmUseCase(prompt)
            if (result != null) {
                _state.update {
                    it.copy(
                        title = result.title,
                        body = result.body,
                        isGenerating = false
                    )
                }
            } else {
                Log.e(TAG, "AI Content generation result is null")
                _state.update {
                    it.copy(
                        isGenerating = false,
                        error = UiText.StringResource(R.string.fcm_error_generation_failed)
                    )
                }
            }
        }
    }

    private fun sendNotification(overrideTarget: NotificationTarget?) {
        viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isLoading = true, error = null, success = null) }

            val notification = com.krumin.tonguecoinsmanager.domain.model.FcmNotification(
                title = currentState.title,
                body = currentState.body,
                imageUrl = currentState.imageUrl?.takeIf { it.isNotBlank() },
                data = currentState.dataPayload,
                priority = currentState.priority,
                androidChannelId = currentState.androidChannelId,
                soundEnabled = currentState.soundEnabled,
                badgeCount = currentState.badgeCount,
                scheduledTime = if (currentState.isScheduled) {
                    parseScheduledTime(currentState.scheduledDate, currentState.scheduledTime)
                } else null,
                target = overrideTarget
                    ?: if (currentState.selectedPlatforms.isNotEmpty() && currentState.target is NotificationTarget.Topic && currentState.target.name == "all") {
                        val condition = currentState.selectedPlatforms.joinToString(" || ") {
                            "'${it.name.lowercase()}' in topics"
                        }
                        NotificationTarget.Condition(condition)
                    } else {
                        currentState.target
                    }
            )

            val result = if (currentState.isScheduled) {
                scheduleFcmUseCase(notification)
            } else {
                sendFcmUseCase(notification, currentState.isDryRun)
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()
                        ?.let { e -> UiText.DynamicString(e.message ?: "Unknown error") },
                    success = if (result.isSuccess) {
                        if (currentState.isScheduled) UiText.StringResource(R.string.fcm_success_scheduled)
                        else if (currentState.isDryRun) UiText.StringResource(R.string.fcm_success_dry_run)
                        else UiText.StringResource(R.string.fcm_success_sent)
                    } else null
                )
            }
        }
    }

    private fun parseScheduledTime(datePart: String, timePart: String): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val date = format.parse("$datePart $timePart")
            date?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun addTestDevice(name: String, token: String) {
        viewModelScope.launch {
            manageTestDevicesUseCase.addDevice(token, name)
        }
    }

    private fun removeTestDevice(device: TestDevice) {
        viewModelScope.launch {
            manageTestDevicesUseCase.removeDevice(device)
        }
    }
}
