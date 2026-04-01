package com.krumin.tonguecoinsmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FcmRequest(
    val message: FcmMessage,
    val validate_only: Boolean = false
)

@Serializable
data class FcmMessage(
    val notification: FcmNotification? = null,
    val data: Map<String, String>? = null,
    val android: AndroidConfig? = null,
    val apns: ApnsConfig? = null,
    val token: String? = null,
    val topic: String? = null,
    val condition: String? = null
)

@Serializable
data class FcmNotification(
    val title: String,
    val body: String,
    val image: String? = null
)

@Serializable
data class AndroidConfig(
    val priority: String = "high",
    val notification: AndroidNotification? = null
)

@Serializable
data class AndroidNotification(
    val sound: String? = "default",
    val click_action: String? = null
)

@Serializable
data class ApnsConfig(
    val payload: ApnsPayload? = null
)

@Serializable
data class ApnsPayload(
    val aps: Aps? = null
)

@Serializable
data class Aps(
    val sound: String? = "default"
)

@Serializable
data class FcmResponse(
    val name: String? = null,
    val error: FcmError? = null
)

@Serializable
data class FcmError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
