package com.krumin.tonguecoinsmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_changes")
data class PendingChangeEntity(
    @PrimaryKey val id: String,
    val type: ChangeType,
    val metadataJson: String,
    val imageFilePath: String?,
    val createdAt: Long = System.currentTimeMillis()
)
