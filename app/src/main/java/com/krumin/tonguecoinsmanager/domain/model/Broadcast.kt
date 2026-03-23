package com.krumin.tonguecoinsmanager.domain.model

data class Broadcast(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val ctaText: String? = null,
    val ctaUrl: String? = null,
    val disabled: Boolean = false
)
