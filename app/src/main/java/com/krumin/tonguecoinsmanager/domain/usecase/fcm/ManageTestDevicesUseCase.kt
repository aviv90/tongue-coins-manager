package com.krumin.tonguecoinsmanager.domain.usecase.fcm

import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.domain.repository.FcmRepository
import kotlinx.coroutines.flow.Flow

class ManageTestDevicesUseCase(
    private val repository: FcmRepository
) {
    fun getDevices(): Flow<List<TestDevice>> = repository.getTestDevices()

    suspend fun addDevice(token: String, name: String): Result<Unit> {
        if (token.isBlank()) return Result.failure(Exception("Token cannot be empty"))
        if (name.isBlank()) return Result.failure(Exception("Name cannot be empty"))

        repository.addTestDevice(TestDevice(token, name))
        return Result.success(Unit)
    }

    suspend fun removeDevice(device: TestDevice) {
        repository.removeTestDevice(device)
    }
}
