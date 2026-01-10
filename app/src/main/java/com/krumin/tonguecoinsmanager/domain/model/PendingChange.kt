package com.krumin.tonguecoinsmanager.domain.model

import java.io.File

sealed class PendingChange {
    abstract val id: String
    abstract val metadata: PhotoMetadata

    data class Add(
        override val id: String,
        override val metadata: PhotoMetadata,
        val imageFile: File
    ) : PendingChange()

    data class Edit(
        override val id: String,
        override val metadata: PhotoMetadata,
        val newImageFile: File? = null
    ) : PendingChange()

    data class Delete(
        override val id: String,
        override val metadata: PhotoMetadata
    ) : PendingChange()
}
