package com.krumin.tonguecoinsmanager.domain.model

data class DailyRiddle(
    val contentItemId: String,
    val date: String,
    val manuallySet: Boolean,
    val createdAt: Long
)
