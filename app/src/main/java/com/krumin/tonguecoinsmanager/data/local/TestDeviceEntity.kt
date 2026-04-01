package com.krumin.tonguecoinsmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_devices")
data class TestDeviceEntity(
    @PrimaryKey
    val token: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)
