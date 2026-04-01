package com.krumin.tonguecoinsmanager.domain.repository

import com.krumin.tonguecoinsmanager.domain.model.FcmNotification
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import kotlinx.coroutines.flow.Flow

interface FcmRepository {
    suspend fun sendNotification(
        notification: FcmNotification,
        dryRun: Boolean = false
    ): Result<Unit>

    // Test Devices
    fun getTestDevices(): Flow<List<TestDevice>>
    suspend fun addTestDevice(device: TestDevice)
    suspend fun removeTestDevice(device: TestDevice)
}
