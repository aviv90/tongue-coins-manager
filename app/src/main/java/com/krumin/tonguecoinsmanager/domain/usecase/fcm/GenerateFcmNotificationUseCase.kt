package com.krumin.tonguecoinsmanager.domain.usecase.fcm

import com.krumin.tonguecoinsmanager.domain.service.FcmGenerator
import com.krumin.tonguecoinsmanager.domain.service.GeneratedFcmContent

class GenerateFcmNotificationUseCase(
    private val fcmGenerator: FcmGenerator
) {
    suspend operator fun invoke(idea: String): GeneratedFcmContent? {
        if (idea.isBlank()) return null
        return fcmGenerator.generateContent(idea)
    }
}
