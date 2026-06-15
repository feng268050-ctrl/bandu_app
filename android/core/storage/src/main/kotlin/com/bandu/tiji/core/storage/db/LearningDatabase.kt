package com.bandu.tiji.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bandu.tiji.core.storage.db.dao.CollectionDao
import com.bandu.tiji.core.storage.db.dao.ErrorItemDao
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity

@Database(
    entities = [
        CollectionEntity::class,
        ErrorItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao

    abstract fun errorItemDao(): ErrorItemDao
}
