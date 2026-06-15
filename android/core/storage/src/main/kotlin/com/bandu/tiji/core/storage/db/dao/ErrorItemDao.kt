package com.bandu.tiji.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ErrorItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(errorItem: ErrorItemEntity)

    @Update
    suspend fun update(errorItem: ErrorItemEntity): Int

    @Delete
    suspend fun delete(errorItem: ErrorItemEntity): Int

    @Query("SELECT * FROM error_items WHERE id = :id")
    suspend fun getById(id: String): ErrorItemEntity?

    @Query(
        """
        SELECT * FROM error_items
        WHERE collection_id = :collectionId
        ORDER BY updated_at DESC, id
        """,
    )
    fun observeByCollection(collectionId: String): Flow<List<ErrorItemEntity>>

    @Query("SELECT COUNT(*) FROM error_items")
    suspend fun count(): Int

    @Query(
        """
        SELECT error_items.* FROM error_items
        INNER JOIN error_item_fts
            ON error_items.rowid = error_item_fts.rowid
        WHERE error_item_fts MATCH :ftsQuery
        ORDER BY error_items.updated_at DESC, error_items.id
        """,
    )
    suspend fun search(ftsQuery: String): List<ErrorItemEntity>
}
