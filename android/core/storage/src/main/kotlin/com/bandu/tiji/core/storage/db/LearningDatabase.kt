package com.bandu.tiji.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bandu.tiji.core.storage.db.dao.CollectionDao
import com.bandu.tiji.core.storage.db.entity.CollectionEntity

@Database(
    entities = [
        CollectionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
}
