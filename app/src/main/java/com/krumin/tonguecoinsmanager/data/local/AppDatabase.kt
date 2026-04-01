package com.krumin.tonguecoinsmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PendingChangeEntity::class,
        TestDeviceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingChangeDao(): PendingChangeDao
    abstract fun testDeviceDao(): TestDeviceDao
}
