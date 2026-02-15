package com.krumin.tonguecoinsmanager.domain.usecase.dailyriddle

import com.krumin.tonguecoinsmanager.domain.model.DailyRiddle
import com.krumin.tonguecoinsmanager.domain.repository.DailyRiddleRepository
import kotlinx.coroutines.flow.Flow

class GetDailyRiddleUseCase(private val repository: DailyRiddleRepository) {
    operator fun invoke(date: String): Flow<DailyRiddle?> {
        return repository.getDailyRiddleFlow(date)
    }
}
