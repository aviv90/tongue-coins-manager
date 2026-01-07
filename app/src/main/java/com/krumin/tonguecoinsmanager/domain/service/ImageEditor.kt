package com.krumin.tonguecoinsmanager.domain.service

interface ImageEditor {
    suspend fun editImage(currentImageBytes: ByteArray, prompt: String): ByteArray
}
