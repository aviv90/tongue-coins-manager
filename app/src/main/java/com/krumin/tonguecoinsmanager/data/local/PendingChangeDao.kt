package com.krumin.tonguecoinsmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingChangeDao {
    @Query("SELECT * FROM pending_changes ORDER BY createdAt")
    fun getAll(): Flow<List<PendingChangeEntity>>

    @Query("SELECT * FROM pending_changes ORDER BY createdAt")
    suspend fun getAllSync(): List<PendingChangeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(change: PendingChangeEntity)

    @Query("DELETE FROM pending_changes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_changes")
    suspend fun deleteAll()
}
