package com.krumin.tonguecoinsmanager.data.model

data class ScheduledFcmEntity(
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val data: Map<String, String> = emptyMap(),
    val targetType: String = "topic", // topic, token, condition
    val targetValue: String = "",
    val priority: String = "HIGH",
    val androidChannelId: String? = "fcm_default_channel",
    val soundEnabled: Boolean = true,
    val badgeCount: Int? = null,
    val scheduledTime: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending",
    val analyticsLabel: String? = null
)
