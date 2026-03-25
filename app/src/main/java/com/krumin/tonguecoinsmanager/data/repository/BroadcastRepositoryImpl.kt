package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.model.BroadcastEntity
import com.krumin.tonguecoinsmanager.domain.model.Broadcast
import com.krumin.tonguecoinsmanager.domain.model.Environment
import com.krumin.tonguecoinsmanager.domain.repository.BroadcastRepository
import com.krumin.tonguecoinsmanager.util.FirestoreResilience.awaitWithRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class BroadcastRepositoryImpl(
    private val context: Context,
    private val firestore: Firestore
) : BroadcastRepository {

    private val collection by lazy { firestore.collection(AppConfig.Firestore.COLLECTION_APP_BROADCASTS) }

    private fun getDocumentId(env: Environment): String {
        return when (env) {
            Environment.PRODUCTION -> AppConfig.Firestore.DOCUMENT_CURRENT
            Environment.TEST -> AppConfig.Firestore.DOCUMENT_TEST
        }
    }

    override suspend fun getBroadcast(env: Environment): Broadcast? = withContext(Dispatchers.IO) {
        val snapshot = awaitWithRetry {
            collection.document(getDocumentId(env)).get()
        }
        if (snapshot.exists()) {
            snapshot.toObject(BroadcastEntity::class.java)?.toDomain()
        } else {
            null
        }
    }

    override fun getBroadcastFlow(env: Environment): Flow<Broadcast?> = callbackFlow {
        val listener = collection.document(getDocumentId(env))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val broadcast = snapshot.toObject(BroadcastEntity::class.java)?.toDomain()
                        trySend(broadcast)
                    } catch (e: Exception) {
                        // Handle deserialization errors
                    }
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveBroadcast(broadcast: Broadcast, env: Environment) =
        withContext(Dispatchers.IO) {
            val entity = broadcast.toEntity()
            awaitWithRetry {
                collection.document(getDocumentId(env))
                    .set(entity, SetOptions.merge())
            }
            Unit
        }

    private fun BroadcastEntity.toDomain(): Broadcast {
        return Broadcast(
            id = id,
            title = title,
            body = body,
            imageUrl = imageUrl,
            ctaText = ctaText,
            ctaUrl = ctaUrl,
            disabled = disabled
        )
    }

    private fun Broadcast.toEntity(): BroadcastEntity {
        return BroadcastEntity(
            id = id,
            title = title,
            body = body,
            imageUrl = imageUrl,
            ctaText = ctaText,
            ctaUrl = ctaUrl,
            disabled = disabled
        )
    }
}
