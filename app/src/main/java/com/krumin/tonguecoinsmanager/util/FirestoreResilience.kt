package com.krumin.tonguecoinsmanager.util

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.common.util.concurrent.MoreExecutors
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility to handle Firestore (gRPC) resilience on Android.
 * Implements silent retries for transient network errors.
 */
object FirestoreResilience {

    private const val MAX_RETRIES = 3
    private const val INITIAL_DELAY = 1000L
    private const val DELAY_MULTIPLIER = 2.0

    /**
     * Await an ApiFuture with built-in retry logic for transient gRPC errors.
     */
    suspend fun <T> awaitWithRetry(
        maxRetries: Int = MAX_RETRIES,
        initialDelay: Long = INITIAL_DELAY,
        call: () -> ApiFuture<T>
    ): T {
        var currentDelay = initialDelay
        var lastException: Throwable? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return awaitFuture(call())
            } catch (e: Exception) {
                lastException = e
                if (isTransient(e) && attempt < maxRetries) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * DELAY_MULTIPLIER).toLong()
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("Unknown error in awaitWithRetry")
    }

    /**
     * Extension-like helper to convert ApiFuture to a suspending call.
     */
    private suspend fun <T> awaitFuture(future: ApiFuture<T>): T =
        suspendCancellableCoroutine { continuation ->
            ApiFutures.addCallback(future, object : ApiFutureCallback<T> {
                override fun onSuccess(result: T) {
                    continuation.resume(result)
                }

                override fun onFailure(t: Throwable) {
                    continuation.resumeWithException(t)
                }
            }, MoreExecutors.directExecutor())

            continuation.invokeOnCancellation {
                future.cancel(true)
            }
        }

    /**
     * Check if the exception is a transient network error that warrants a retry.
     */
    private fun isTransient(e: Throwable): Boolean {
        return when (e) {
            is StatusRuntimeException -> {
                when (e.status.code) {
                    Status.Code.UNAVAILABLE,
                    Status.Code.DEADLINE_EXCEEDED,
                    Status.Code.RESOURCE_EXHAUSTED,
                    Status.Code.ABORTED -> true
                    else -> false
                }
            }
            else -> {
                val message = e.message ?: ""
                message.contains("Unable to resolve host", ignoreCase = true) ||
                        message.contains("Timeout", ignoreCase = true) ||
                        message.contains("Connection reset", ignoreCase = true)
            }
        }
    }
}
