package com.krumin.tonguecoinsmanager.domain.repository

import com.krumin.tonguecoinsmanager.domain.model.DailyRiddle
import kotlinx.coroutines.flow.Flow

interface DailyRiddleRepository {
    suspend fun setDailyRiddle(date: String, contentItemId: String)
    suspend fun resetDailyRiddle(date: String)
    suspend fun getDailyRiddle(date: String): DailyRiddle?
    fun getDailyRiddleFlow(date: String): Flow<DailyRiddle?>
}
