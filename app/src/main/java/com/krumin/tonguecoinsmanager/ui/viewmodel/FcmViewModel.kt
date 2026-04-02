package com.krumin.tonguecoinsmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.repository.FcmDraftRepository
import com.krumin.tonguecoinsmanager.domain.model.FcmNotification
import com.krumin.tonguecoinsmanager.domain.model.FcmPriority
import com.krumin.tonguecoinsmanager.domain.model.NotificationTarget
import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata
import com.krumin.tonguecoinsmanager.domain.model.Platform
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.domain.repository.PhotoRepository
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.GenerateFcmNotificationUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.ManageTestDevicesUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.ScheduleFcmNotificationUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.SendFcmNotificationUseCase
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
    val selectedPlatforms: Set<Platform> = Platform.entries.toSet(),
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
    val androidChannelId: String? = AppConfig.Fcm.DEFAULT_CHANNEL_ID,
    val soundEnabled: Boolean = true,
    val badgeCount: Int? = null,

    // Scheduling
    val isScheduled: Boolean = false,
    val scheduledDate: String = SimpleDateFormat(
        AppConfig.Fcm.DATE_FORMAT,
        Locale.US
    ).format(Date()),
    val scheduledTime: String = SimpleDateFormat(
        AppConfig.Fcm.TIME_FORMAT,
        Locale.US
    ).format(Date()),

    // Riddle Picker
    val availableRiddles: List<PhotoMetadata> = emptyList(),
    val isRiddlePickerVisible: Boolean = false
)

sealed interface FcmAction {
    data class UpdateTitle(val title: String) : FcmAction
    data class UpdateBody(val body: String) : FcmAction
    data class UpdateImageUrl(val url: String?) : FcmAction
    data class AddDataPair(val key: String, val value: String) : FcmAction
    data class RemoveDataPair(val key: String) : FcmAction
    data class TogglePlatform(val platform: Platform) : FcmAction
    data class UpdateSelectedPlatforms(val platforms: Set<Platform>) :
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

    // Riddle Picker
    data class ToggleRiddlePicker(val visible: Boolean) : FcmAction
    data class SelectRiddle(val riddle: PhotoMetadata) : FcmAction

    object ClearStatus : FcmAction
    object ClearForm : FcmAction
}

class FcmViewModel(
    private val sendFcmUseCase: SendFcmNotificationUseCase,
    private val scheduleFcmUseCase: ScheduleFcmNotificationUseCase,
    private val generateFcmUseCase: GenerateFcmNotificationUseCase,
    private val manageTestDevicesUseCase: ManageTestDevicesUseCase,
    private val photoRepository: PhotoRepository,
    private val draftRepository: FcmDraftRepository
) : ViewModel() {

    companion object {
        private const val TAG = "FcmViewModel"
    }

    private val _state = MutableStateFlow(FcmState())
    val state = _state.asStateFlow()

    init {
        // Load Draft
        draftRepository.loadDraft()?.let { draft ->
            _state.update {
                it.copy(
                    title = draft.title,
                    body = draft.body,
                    imageUrl = draft.imageUrl,
                    dataPayload = draft.dataPayload,
                    selectedPlatforms = if (draft.selectedPlatforms.isEmpty()) Platform.entries.toSet()
                    else draft.selectedPlatforms
                )
            }
        }

        viewModelScope.launch {
            manageTestDevicesUseCase.getDevices().collect { devices ->
                _state.update { it.copy(testDevices = devices) }
            }
        }

        // Load Riddles for Picker
        viewModelScope.launch {
            val photos = photoRepository.getPhotos()
            _state.update { it.copy(availableRiddles = photos) }
        }
    }

    private fun saveDraft() {
        val s = _state.value
        draftRepository.saveDraft(
            FcmDraftRepository.FcmDraft(
                title = s.title,
                body = s.body,
                imageUrl = s.imageUrl,
                dataPayload = s.dataPayload,
                selectedPlatforms = s.selectedPlatforms
            )
        )
    }

    fun handleAction(action: FcmAction) {
        when (action) {
            is FcmAction.UpdateTitle -> {
                _state.update { it.copy(title = action.title) }
                saveDraft()
            }

            is FcmAction.UpdateBody -> {
                _state.update { it.copy(body = action.body) }
                saveDraft()
            }

            is FcmAction.UpdateImageUrl -> {
                _state.update { it.copy(imageUrl = action.url) }
                saveDraft()
            }

            is FcmAction.AddDataPair -> {
                _state.update {
                    it.copy(dataPayload = it.dataPayload + (action.key to action.value))
                }
                saveDraft()
            }

            is FcmAction.RemoveDataPair -> {
                _state.update {
                    it.copy(dataPayload = it.dataPayload - action.key)
                }
                saveDraft()
            }

            is FcmAction.TogglePlatform -> {
                _state.update {
                    val newPlatforms = if (it.selectedPlatforms.contains(action.platform)) {
                        it.selectedPlatforms - action.platform
                    } else {
                        it.selectedPlatforms + action.platform
                    }
                    it.copy(selectedPlatforms = newPlatforms)
                }
                saveDraft()
            }

            is FcmAction.UpdateSelectedPlatforms -> {
                _state.update { it.copy(selectedPlatforms = action.platforms) }
                saveDraft()
            }

            is FcmAction.UpdateDryRun -> {
                _state.update { it.copy(isDryRun = action.enabled) }
                saveDraft()
            }

            is FcmAction.UpdateIdeaPrompt -> {
                _state.update { it.copy(ideaPrompt = action.prompt) }
                saveDraft()
            }

            FcmAction.GenerateContent -> generateContent()
            is FcmAction.UpdatePriority -> {
                _state.update { it.copy(priority = action.priority) }
                saveDraft()
            }

            is FcmAction.UpdateChannelId -> {
                _state.update { it.copy(androidChannelId = action.channelId) }
                saveDraft()
            }

            is FcmAction.UpdateSoundEnabled -> {
                _state.update { it.copy(soundEnabled = action.enabled) }
                saveDraft()
            }

            is FcmAction.UpdateBadgeCount -> {
                _state.update { it.copy(badgeCount = action.count) }
                saveDraft()
            }

            is FcmAction.UpdateIsScheduled -> {
                _state.update { it.copy(isScheduled = action.enabled) }
                saveDraft()
            }

            is FcmAction.UpdateScheduledDate -> {
                _state.update { it.copy(scheduledDate = action.date) }
                saveDraft()
            }

            is FcmAction.UpdateScheduledTime -> {
                _state.update { it.copy(scheduledTime = action.time) }
                saveDraft()
            }

            is FcmAction.SendNotification -> sendNotification(action.overrideTarget)
            is FcmAction.AddTestDevice -> addTestDevice(action.name, action.token)
            is FcmAction.RemoveTestDevice -> removeTestDevice(action.device)
            FcmAction.ClearStatus -> _state.update { it.copy(error = null, success = null) }
            FcmAction.ClearForm -> {
                _state.update {
                    FcmState(
                        testDevices = it.testDevices,
                        availableRiddles = it.availableRiddles,
                        scheduledDate = SimpleDateFormat(
                            AppConfig.Fcm.DATE_FORMAT,
                            Locale.US
                        ).format(Date()),
                        scheduledTime = SimpleDateFormat(
                            AppConfig.Fcm.TIME_FORMAT,
                            Locale.US
                        ).format(Date())
                    )
                }
                draftRepository.clearDraft()
            }

            is FcmAction.ToggleRiddlePicker -> _state.update { it.copy(isRiddlePickerVisible = action.visible) }
            is FcmAction.SelectRiddle -> {
                _state.update {
                    it.copy(
                        imageUrl = action.riddle.imageUrl,
                        isRiddlePickerVisible = false
                    )
                }
                saveDraft()
            }
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
                saveDraft()
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

            val notification = FcmNotification(
                title = currentState.title,
                body = currentState.body,
                imageUrl = currentState.imageUrl?.takeIf { it.isNotBlank() },
                data = currentState.dataPayload,
                priority = currentState.priority,
                androidChannelId = currentState.androidChannelId,
                soundEnabled = currentState.soundEnabled,
                badgeCount = currentState.badgeCount,
                scheduledTime = if (currentState.isScheduled) {
                    val time =
                        parseScheduledTime(currentState.scheduledDate, currentState.scheduledTime)
                    Log.d(
                        TAG,
                        "Scheduling notification for timestamp: $time (${currentState.scheduledDate} ${currentState.scheduledTime})"
                    )
                    time
                } else null,
                target = overrideTarget ?: when {
                    currentState.selectedPlatforms.size == 2 -> {
                        // Everyone - Use "all" topic as it is confirmed to reach 2000+ users
                        NotificationTarget.Topic("all")
                    }

                    currentState.selectedPlatforms.contains(Platform.ANDROID) -> {
                        NotificationTarget.Topic("android")
                    }

                    currentState.selectedPlatforms.contains(Platform.IOS) -> {
                        NotificationTarget.Topic("ios")
                    }

                    else -> {
                        NotificationTarget.Topic("all")
                    }
                },
                analyticsLabel = when {
                    overrideTarget != null -> "test_device"
                    currentState.selectedPlatforms.size == 2 -> "production_all"
                    currentState.selectedPlatforms.contains(Platform.ANDROID) -> "production_android"
                    currentState.selectedPlatforms.contains(Platform.IOS) -> "production_ios"
                    else -> "production_unknown"
                }
            )

            Log.d(
                TAG,
                "Sending notification with target: ${notification.target} (Override: ${overrideTarget != null})"
            )

            val result = if (currentState.isScheduled) {
                Log.i(TAG, "Executing ScheduleFcmUseCase")
                scheduleFcmUseCase(notification)
            } else {
                Log.i(TAG, "Executing SendFcmUseCase (Immediate)")
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
            val format = SimpleDateFormat(
                AppConfig.Fcm.DATETIME_FORMAT,
                Locale.US
            )
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
