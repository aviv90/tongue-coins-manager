package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.Firestore
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.local.TestDeviceDao
import com.krumin.tonguecoinsmanager.data.local.TestDeviceEntity
import com.krumin.tonguecoinsmanager.data.model.AndroidConfig
import com.krumin.tonguecoinsmanager.data.model.AndroidNotification
import com.krumin.tonguecoinsmanager.data.model.ApnsConfig
import com.krumin.tonguecoinsmanager.data.model.ApnsHeaders
import com.krumin.tonguecoinsmanager.data.model.ApnsPayload
import com.krumin.tonguecoinsmanager.data.model.Aps
import com.krumin.tonguecoinsmanager.data.model.FcmMessage
import com.krumin.tonguecoinsmanager.data.model.FcmOptions
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val tokenMutex = Mutex()

    // Loaded once from assets; ServiceAccountCredentials gives us both projectId and auth.
    private val serviceAccount: ServiceAccountCredentials by lazy {
        context.assets.open(AppConfig.Gcs.DEFAULT_KEY_FILE).use { stream ->
            ServiceAccountCredentials.fromStream(stream)
        }
    }

    private val scopedCredentials: GoogleCredentials by lazy {
        serviceAccount.createScoped(listOf(AppConfig.Fcm.SCOPE))
    }

    private val projectId: String by lazy { serviceAccount.projectId }

    override suspend fun sendNotification(
        notification: FcmNotification,
        dryRun: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            val fcmUrl = "${AppConfig.Fcm.BASE_URL}/$projectId/messages:send"

            val requestBody = FcmRequest(
                message = notification.toApiMessage(),
                validate_only = dryRun
            )

            val jsonBody = json.encodeToString(requestBody)
            Log.d("FcmRepository", "Sending FCM: ${if (dryRun) "[DRY RUN] " else ""}$jsonBody")

            val request = Request.Builder()
                .url(fcmUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", AppConfig.Fcm.CONTENT_TYPE_JSON)
                .post(jsonBody.toRequestBody(AppConfig.Fcm.CONTENT_TYPE_JSON.toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body.string()
                if (response.isSuccessful) {
                    Log.d("FcmRepository", "FCM send successful: $responseBody")
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("FCM failed ${response.code}: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        tokenMutex.withLock {
            retryOnTransient {
                scopedCredentials.refreshIfExpired()
                scopedCredentials.accessToken.tokenValue
            }
        }
    }

    private suspend fun <T> retryOnTransient(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        call: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var lastException: Throwable? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return call()
            } catch (e: Exception) {
                lastException = e
                if (isTransient(e) && attempt < maxRetries) {
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * 2.0).toLong()
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("Retry failed")
    }

    private fun isTransient(e: Throwable): Boolean {
        val message = e.message ?: ""
        return message.contains("Unable to resolve host", ignoreCase = true) ||
                message.contains("Timeout", ignoreCase = true) ||
                message.contains("Connection reset", ignoreCase = true) ||
                e is IOException
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
                    scheduledTime = notification.scheduledTime ?: 0L,
                    analyticsLabel = notification.analyticsLabel
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

    override fun getTestDevices(): Flow<List<TestDevice>> =
        testDeviceDao.getAllFlow().map { entities ->
            entities.map { TestDevice(it.token, it.name) }
        }

    override suspend fun addTestDevice(device: TestDevice) {
        testDeviceDao.insert(TestDeviceEntity(device.token, device.name))
    }

    override suspend fun removeTestDevice(device: TestDevice) {
        testDeviceDao.delete(TestDeviceEntity(device.token, device.name))
    }

    private fun FcmNotification.toApiMessage(): FcmMessage = FcmMessage(
        notification = FcmApiNotification(title = title, body = body, image = imageUrl),
        data = data.takeIf { it.isNotEmpty() },
        android = AndroidConfig(
            priority = if (priority == FcmPriority.HIGH) "high" else "normal",
            notification = AndroidNotification(
                channel_id = androidChannelId,
                sound = if (soundEnabled) "default" else null
            )
        ),
        apns = ApnsConfig(
            headers = ApnsHeaders(apns_priority = if (priority == FcmPriority.HIGH) "10" else "5"),
            payload = ApnsPayload(
                aps = Aps(
                    sound = if (soundEnabled) "default" else null,
                    badge = badgeCount,
                    mutable_content = if (imageUrl != null) 1 else null
                )
            )
        ),
        fcm_options = analyticsLabel?.let { FcmOptions(it) },
        token = (target as? NotificationTarget.Token)?.token,
        topic = (target as? NotificationTarget.Topic)?.name,
        condition = (target as? NotificationTarget.Condition)?.condition
    )
}
