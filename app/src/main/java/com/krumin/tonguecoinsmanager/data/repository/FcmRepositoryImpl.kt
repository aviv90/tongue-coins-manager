package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.local.TestDeviceDao
import com.krumin.tonguecoinsmanager.data.local.TestDeviceEntity
import com.krumin.tonguecoinsmanager.data.model.AndroidConfig
import com.krumin.tonguecoinsmanager.data.model.AndroidNotification
import com.krumin.tonguecoinsmanager.data.model.FcmMessage
import com.krumin.tonguecoinsmanager.data.model.FcmRequest
import com.krumin.tonguecoinsmanager.domain.model.FcmNotification
import com.krumin.tonguecoinsmanager.domain.model.NotificationTarget
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.domain.repository.FcmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import com.krumin.tonguecoinsmanager.data.model.FcmNotification as FcmApiNotification

class FcmRepositoryImpl(
    private val context: Context,
    private val testDeviceDao: TestDeviceDao,
    private val okHttpClient: OkHttpClient
) : FcmRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val fcmScope = "https://www.googleapis.com/auth/firebase.messaging"

    private suspend fun getProjectId(): String = withContext(Dispatchers.IO) {
        val stream = context.assets.open(AppConfig.Gcs.DEFAULT_KEY_FILE)
        val creds = com.google.auth.oauth2.ServiceAccountCredentials.fromStream(stream)
        creds.projectId
    }

    override suspend fun sendNotification(
        notification: FcmNotification,
        dryRun: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            val projectId = getProjectId()
            val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

            val requestBody = FcmRequest(
                message = notification.toApiMessage(),
                validate_only = dryRun
            )

            val jsonBody = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(fcmUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.body?.string()
                    Result.failure(IOException("FCM failed with status ${response.code}: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val stream = context.assets.open(AppConfig.Gcs.DEFAULT_KEY_FILE)
        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(listOf(fcmScope))
        credentials.refreshIfExpired()
        credentials.accessToken.tokenValue
    }

    override fun getTestDevices(): Flow<List<TestDevice>> {
        return testDeviceDao.getAllFlow().map { entities ->
            entities.map { TestDevice(it.token, it.name) }
        }
    }

    override suspend fun addTestDevice(device: TestDevice) {
        testDeviceDao.insert(TestDeviceEntity(device.token, device.name))
    }

    override suspend fun removeTestDevice(device: TestDevice) {
        testDeviceDao.delete(TestDeviceEntity(device.token, device.name))
    }

    private fun FcmNotification.toApiMessage(): FcmMessage {
        val fcmNotification = FcmApiNotification(
            title = title,
            body = body,
            image = imageUrl
        )

        return FcmMessage(
            notification = fcmNotification,
            data = data,
            android = AndroidConfig(
                notification = AndroidNotification(
                    click_action = null // Let the OS decide (opens launcher activity)
                )
            ),
            token = when (target) {
                is NotificationTarget.Token -> target.token
                else -> null
            },
            topic = when (target) {
                is NotificationTarget.Topic -> target.name
                else -> null
            },
            condition = when (target) {
                is NotificationTarget.Condition -> target.condition
                else -> null
            }
        )
    }
}
