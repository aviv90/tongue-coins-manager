package com.krumin.tonguecoinsmanager.data.model

import java.util.Date

data class DailyRiddleEntity(
    val contentItemId: String = "",
    val date: String = "",
    val manuallySet: Boolean = false,
    val createdAt: Date? = null
)
