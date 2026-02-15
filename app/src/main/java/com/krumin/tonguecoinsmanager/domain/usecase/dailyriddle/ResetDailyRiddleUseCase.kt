package com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle

import com.krumin.tonguecoinsmanager.domain.repository.DailyRiddleRepository

class ResetDailyRiddleUseCase(private val repository: DailyRiddleRepository) {
    suspend operator fun invoke(date: String) {
        repository.resetDailyRiddle(date)
    }
}
