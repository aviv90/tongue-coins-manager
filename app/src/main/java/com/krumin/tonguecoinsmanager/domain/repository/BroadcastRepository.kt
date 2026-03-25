package com.krumin.tonguecoinsmanager.domain.repository

import com.krumin.tonguecoinsmanager.domain.model.Broadcast
import com.krumin.tonguecoinsmanager.domain.model.Environment
import kotlinx.coroutines.flow.Flow

interface BroadcastRepository {
    suspend fun getBroadcast(env: Environment): Broadcast?
    fun getBroadcastFlow(env: Environment): Flow<Broadcast?>
    suspend fun saveBroadcast(broadcast: Broadcast, env: Environment)
}
