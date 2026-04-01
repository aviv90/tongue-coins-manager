package com.krumin.tonguecoinsmanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDeviceDao {
    @Query("SELECT * FROM test_devices ORDER BY addedAt DESC")
    fun getAllFlow(): Flow<List<TestDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: TestDeviceEntity)

    @Delete
    suspend fun delete(device: TestDeviceEntity)

    @Query("SELECT * FROM test_devices WHERE token = :token")
    suspend fun getByToken(token: String): TestDeviceEntity?
}
