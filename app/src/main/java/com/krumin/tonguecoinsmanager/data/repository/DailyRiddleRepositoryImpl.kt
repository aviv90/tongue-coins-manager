package com.krumin.tonguecoinsmanager.data.repository

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.SetOptions
import com.krumin.tonguecoinsmanager.data.infrastructure.AppConfig
import com.krumin.tonguecoinsmanager.data.model.DailyRiddleEntity
import com.krumin.tonguecoinsmanager.domain.model.DailyRiddle
import com.krumin.tonguecoinsmanager.domain.repository.DailyRiddleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.Date

class DailyRiddleRepositoryImpl(
    private val context: Context,
    private val keyFileName: String = AppConfig.Gcs.DEFAULT_KEY_FILE
) : DailyRiddleRepository {

    private val firestore: Firestore by lazy {
        val options = FirestoreOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(context.assets.open(keyFileName)))
            .setDatabaseId(AppConfig.Firestore.DATABASE_ID)
            .build()
        options.service
    }

    private val collection by lazy { firestore.collection(COLLECTION_NAME) }

    override suspend fun setDailyRiddle(date: String, contentItemId: String) =
        withContext(Dispatchers.IO) {
            val entity = DailyRiddleEntity(
                contentItemId = contentItemId,
                date = date,
                manuallySet = true,
                createdAt = Date() // Use java.util.Date directly
            )
            collection.document(date).set(entity, SetOptions.merge())
                .get() // .get() blocks until complete
            Unit
        }

    override suspend fun resetDailyRiddle(date: String) = withContext(Dispatchers.IO) {
        collection.document(date).delete().get()
        Unit
    }

    override suspend fun getDailyRiddle(date: String): DailyRiddle? = withContext(Dispatchers.IO) {
        val snapshot = collection.document(date).get().get()
        if (snapshot.exists()) {
            snapshot.toObject(DailyRiddleEntity::class.java)?.toDomain()
        } else {
            null
        }
    }

    override fun getDailyRiddleFlow(date: String): Flow<DailyRiddle?> = callbackFlow {
        val listener = collection.document(date).addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    val riddle = snapshot.toObject(DailyRiddleEntity::class.java)?.toDomain()
                    trySend(riddle)
                } catch (e: Exception) {
                    // Handle deserialization errors
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    private fun DailyRiddleEntity.toDomain(): DailyRiddle {
        return DailyRiddle(
            contentItemId = contentItemId,
            date = date,
            manuallySet = manuallySet,
            createdAt = createdAt?.time ?: 0L
        )
    }

    companion object {
        private const val COLLECTION_NAME = AppConfig.Firestore.COLLECTION_DAILY_RIDDLES
    }
}
