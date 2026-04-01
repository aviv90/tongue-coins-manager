package com.krumin.tonguecoinsmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krumin.tonguecoinsmanager.R
import com.krumin.tonguecoinsmanager.domain.model.FcmNotification
import com.krumin.tonguecoinsmanager.domain.model.NotificationTarget
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.ManageTestDevicesUseCase
import com.krumin.tonguecoinsmanager.domain.usecase.fcm.SendFcmNotificationUseCase
import com.krumin.tonguecoinsmanager.util.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val isDryRun: Boolean = false
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

    // Test Devices
    data class AddTestDevice(val name: String, val token: String) : FcmAction
    data class RemoveTestDevice(val device: TestDevice) : FcmAction

    object ClearStatus : FcmAction
}

class FcmViewModel(
    private val sendFcmUseCase: SendFcmNotificationUseCase,
    private val manageTestDevicesUseCase: ManageTestDevicesUseCase
) : ViewModel() {

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
            is FcmAction.SendNotification -> sendNotification(action.overrideTarget)
            is FcmAction.AddTestDevice -> addTestDevice(action.name, action.token)
            is FcmAction.RemoveTestDevice -> removeTestDevice(action.device)
            FcmAction.ClearStatus -> _state.update { it.copy(error = null, success = null) }
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

            val result = sendFcmUseCase(notification, currentState.isDryRun)

            _state.update {
                it.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()
                        ?.let { e -> UiText.DynamicString(e.message ?: "Unknown error") },
                    success = if (result.isSuccess) {
                        if (currentState.isDryRun) UiText.StringResource(R.string.fcm_success_dry_run)
                        else UiText.StringResource(R.string.fcm_success_sent)
                    } else null
                )
            }
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
