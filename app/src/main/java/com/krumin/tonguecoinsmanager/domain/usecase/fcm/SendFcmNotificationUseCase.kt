package com.krumin.tonguecoinsmanager.domain.usecase.fcm

import com.krumin.tonguecoinsmanager.domain.model.FcmNotification
import com.krumin.tonguecoinsmanager.domain.repository.FcmRepository

class SendFcmNotificationUseCase(
    private val repository: FcmRepository
) {
    suspend operator fun invoke(
        notification: FcmNotification,
        dryRun: Boolean = false
    ): Result<Unit> {
        if (notification.title.isBlank()) return Result.failure(Exception("Title cannot be empty"))
        if (notification.body.isBlank()) return Result.failure(Exception("Body cannot be empty"))

        return repository.sendNotification(notification, dryRun)
    }
}
