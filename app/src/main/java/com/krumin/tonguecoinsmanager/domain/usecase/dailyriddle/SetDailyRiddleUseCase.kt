package com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle

import com.krumin.tonguecoinsmanager.domain.repository.DailyRiddleRepository

class SetDailyRiddleUseCase(private val repository: DailyRiddleRepository) {
    suspend operator fun invoke(date: String, contentItemId: String) {
        repository.setDailyRiddle(date, contentItemId)
    }
}
