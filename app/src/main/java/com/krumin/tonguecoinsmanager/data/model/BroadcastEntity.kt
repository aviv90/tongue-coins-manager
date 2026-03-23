package com.krumin.tonguecoinsmanager.data.model

data class BroadcastEntity(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val ctaText: String? = null,
    val ctaUrl: String? = null,
    val disabled: Boolean = false
)
