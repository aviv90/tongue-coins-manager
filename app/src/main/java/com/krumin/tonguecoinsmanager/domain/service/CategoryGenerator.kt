package com.krumin.tonguecoinsmanager.domain.service

import com.krumin.tonguecoinsmanager.domain.model.PhotoMetadata

interface CategoryGenerator {
    suspend fun generateCategories(
        title: String,
        contextPhotos: List<PhotoMetadata>
    ): List<String>
}
