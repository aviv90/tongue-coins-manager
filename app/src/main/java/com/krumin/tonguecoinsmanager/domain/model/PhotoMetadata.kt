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
    val version: Int = 1
)
