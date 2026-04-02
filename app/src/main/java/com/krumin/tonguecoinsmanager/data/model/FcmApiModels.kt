package com.krumin.tonguecoinsmanager.data.model

import kotlinx.serialization.SerialName
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
    val fcm_options: FcmOptions? = null,
    val token: String? = null,
    val topic: String? = null,
    val condition: String? = null
)

@Serializable
data class FcmOptions(
    val analytics_label: String? = null
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
    val ttl: String = "604800s",
    val notification: AndroidNotification? = null
)

@Serializable
data class AndroidNotification(
    val sound: String? = "default",
    val click_action: String? = null,
    val channel_id: String? = null,
    val image: String? = null
)

@Serializable
data class ApnsConfig(
    val headers: ApnsHeaders? = null,
    val payload: ApnsPayload? = null
)

@Serializable
data class ApnsHeaders(
    @SerialName("apns-priority") val apns_priority: String? = null
)

@Serializable
data class ApnsPayload(
    val aps: Aps? = null
)

@Serializable
data class Aps(
    val sound: String? = "default",
    val badge: Int? = null,
    @SerialName("mutable-content") val mutable_content: Int? = null
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
