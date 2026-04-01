package com.krumin.tonguecoinsmanager.domain.service

data class GeneratedFcmContent(
    val title: String,
    val body: String
)

interface FcmGenerator {
    suspend fun generateContent(idea: String): GeneratedFcmContent?
}
