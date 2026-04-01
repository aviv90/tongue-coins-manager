package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.local.TestDeviceDao
import com.krumin.tonguecoinsmanager.data.local.TestDeviceEntity
import com.krumin.tonguecoinsmanager.data.model.AndroidConfig
import com.krumin.tonguecoinsmanager.data.model.AndroidNotification
import com.krumin.tonguecoinsmanager.data.model.ApnsConfig
import com.krumin.tonguecoinsmanager.data.model.ApnsPayload
import com.krumin.tonguecoinsmanager.data.model.Aps
import com.krumin.tonguecoinsmanager.data.model.FcmMessage
import com.krumin.tonguecoinsmanager.data.model.FcmRequest
import com.krumin.tonguecoinsmanager.data.model.ScheduledFcmEntity
import com.krumin.tonguecoinsmanager.domain.model.FcmNotification
import com.krumin.tonguecoinsmanager.domain.model.FcmPriority
import com.krumin.tonguecoinsmanager.domain.model.NotificationTarget
import com.krumin.tonguecoinsmanager.domain.model.TestDevice
import com.krumin.tonguecoinsmanager.domain.repository.FcmRepository
import com.krumin.tonguecoinsmanager.util.FirestoreResilience.awaitWithRetry
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
    private val firestore: Firestore,
    private val testDeviceDao: TestDeviceDao,
    private val okHttpClient: OkHttpClient
) : FcmRepository {

    private val json = Json { ignoreUnknownKeys = true }

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
            val fcmUrl = "${AppConfig.Fcm.BASE_URL}/$projectId/messages:send"

            val requestBody = FcmRequest(
                message = notification.toApiMessage(),
                validate_only = dryRun
            )

            val jsonBody = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(fcmUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", AppConfig.Fcm.CONTENT_TYPE_JSON)
                .post(jsonBody.toRequestBody(AppConfig.Fcm.CONTENT_TYPE_JSON.toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.body.string()
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
            .createScoped(listOf(AppConfig.Fcm.SCOPE))
        credentials.refreshIfExpired()
        credentials.accessToken.tokenValue
    }

    override suspend fun scheduleNotification(notification: FcmNotification): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val entity = ScheduledFcmEntity(
                    title = notification.title,
                    body = notification.body,
                    imageUrl = notification.imageUrl,
                    data = notification.data,
                    targetType = when (notification.target) {
                        is NotificationTarget.Topic -> "topic"
                        is NotificationTarget.Token -> "token"
                        is NotificationTarget.Condition -> "condition"
                    },
                    targetValue = when (val t = notification.target) {
                        is NotificationTarget.Topic -> t.name
                        is NotificationTarget.Token -> t.token
                        is NotificationTarget.Condition -> t.condition
                    },
                    priority = notification.priority.name,
                    androidChannelId = notification.androidChannelId,
                    soundEnabled = notification.soundEnabled,
                    badgeCount = notification.badgeCount,
                    scheduledTime = notification.scheduledTime ?: 0L
                )

                awaitWithRetry {
                    firestore.collection(AppConfig.Firestore.COLLECTION_SCHEDULEED_FCM)
                        .document()
                        .set(entity)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
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
                priority = if (priority == FcmPriority.HIGH) "high" else "normal",
                notification = AndroidNotification(
                    click_action = null, // Let the OS decide (opens launcher activity)
                    channel_id = androidChannelId,
                    sound = if (soundEnabled) "default" else null
                )
            ),
            apns = ApnsConfig(
                payload = ApnsPayload(
                    aps = Aps(
                        sound = if (soundEnabled) "default" else null,
                        badge = badgeCount
                    )
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
