package com.krumin.tonguecoinsmanager.domain.usecase.fcm

import com.krumin.tonguecoinsmanager.domain.model.FcmNotification
import com.krumin.tonguecoinsmanager.domain.repository.FcmRepository

class ScheduleFcmNotificationUseCase(
    private val fcmRepository: FcmRepository
) {
    suspend operator fun invoke(notification: FcmNotification): Result<Unit> {
        if (notification.scheduledTime == null) {
            return Result.failure(IllegalArgumentException("Scheduled time is required"))
        }
        return fcmRepository.scheduleNotification(notification)
    }
}
