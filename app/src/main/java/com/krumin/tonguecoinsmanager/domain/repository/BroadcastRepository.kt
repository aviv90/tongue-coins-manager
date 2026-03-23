package com.krumin.tonguecoinsmanager.domain.repository

import com.krumin.tonguecoinsmanager.domain.model.Broadcast
import kotlinx.coroutines.flow.Flow

interface BroadcastRepository {
    suspend fun getBroadcast(): Broadcast?
    fun getBroadcastFlow(): Flow<Broadcast?>
    suspend fun saveBroadcast(broadcast: Broadcast)
}
