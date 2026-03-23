package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.SetOptions
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.model.BroadcastEntity
import com.krumin.tonguecoinsmanager.domain.model.Broadcast
import com.krumin.tonguecoinsmanager.domain.repository.BroadcastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class BroadcastRepositoryImpl(
    private val context: Context,
    private val keyFileName: String = AppConfig.Gcs.DEFAULT_KEY_FILE
) : BroadcastRepository {

    private val firestore: Firestore by lazy {
        val options = FirestoreOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(context.assets.open(keyFileName)))
            .setDatabaseId(AppConfig.Firestore.DATABASE_ID)
            .build()
        options.service
    }

    private val collection by lazy { firestore.collection(AppConfig.Firestore.COLLECTION_APP_BROADCASTS) }

    override suspend fun getBroadcast(): Broadcast? = withContext(Dispatchers.IO) {
        val snapshot = collection.document(AppConfig.Firestore.DOCUMENT_CURRENT).get().get()
        if (snapshot.exists()) {
            snapshot.toObject(BroadcastEntity::class.java)?.toDomain()
        } else {
            null
        }
    }

    override fun getBroadcastFlow(): Flow<Broadcast?> = callbackFlow {
        val listener = collection.document(AppConfig.Firestore.DOCUMENT_CURRENT)
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

    override suspend fun saveBroadcast(broadcast: Broadcast) = withContext(Dispatchers.IO) {
        val entity = broadcast.toEntity()
        collection.document(AppConfig.Firestore.DOCUMENT_CURRENT)
            .set(entity, SetOptions.merge())
            .get()
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
