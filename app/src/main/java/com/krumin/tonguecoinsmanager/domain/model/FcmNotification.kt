package com.krumin.tonguecoinsmanager.domain.model

data class FcmNotification(
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val data: Map<String, String> = emptyMap(),
    val target: NotificationTarget
)

sealed class NotificationTarget {
    data class Topic(val name: String) : NotificationTarget()
    data class Token(val token: String) : NotificationTarget()
    data class Condition(val condition: String) : NotificationTarget()
}

data class TestDevice(
    val token: String,
    val name: String
)
