package com.bandu.tiji.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bandu.tiji.core.storage.db.dao.CollectionDao
import com.bandu.tiji.core.storage.db.dao.ErrorItemDao
import com.bandu.tiji.core.storage.db.dao.TagDao
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemTagEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity

@Database(
    entities = [
        CollectionEntity::class,
        ErrorItemEntity::class,
        TagEntity::class,
        ErrorItemTagEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao

    abstract fun errorItemDao(): ErrorItemDao

    abstract fun tagDao(): TagDao
}
