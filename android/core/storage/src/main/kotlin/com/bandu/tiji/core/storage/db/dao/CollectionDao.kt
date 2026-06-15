package com.bandu.tiji.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: CollectionEntity)

    @Update
    suspend fun update(collection: CollectionEntity): Int

    @Delete
    suspend fun delete(collection: CollectionEntity): Int

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: String): CollectionEntity?

    @Query("SELECT * FROM collections ORDER BY updated_at DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CollectionEntity>>
}
