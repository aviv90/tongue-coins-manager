package com.krumin.tonguecoinsmanager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoMetadata(
    val id: String,
    val imageUrl: String,
    val title: String,
    val credit: String = "",
    val hint: String = "",
    val difficulty: Int = 1,
    val categories: String = "",
    val version: Int = 1,
    val aspectRatio: Float = 1.0f,
    val supportedPlatforms: List<Platform> = listOf(Platform.ANDROID, Platform.IOS)
) {
    fun trimmed(): PhotoMetadata = copy(
        title = title.trim(),
        credit = credit.trim(),
        hint = hint.trim(),
        categories = categories.trim()
    )
}
