package com.bandu.tiji.core.storage.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.bandu.tiji.core.storage.db.entity.ErrorItemTagEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity

@Dao
abstract class TagDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertEntity(tag: TagEntity)

    @Query(
        """
        SELECT COUNT(*) FROM tags
        WHERE subject = :subject
          AND parent_id IS :parentId
          AND name = :name COLLATE NOCASE
        """,
    )
    protected abstract suspend fun countByIdentity(
        subject: String,
        parentId: String?,
        name: String,
    ): Int

    @Transaction
    open suspend fun insert(tag: TagEntity) {
        check(countByIdentity(tag.subject, tag.parentId, tag.name) == 0) {
            "A tag with this subject, parent, and name already exists"
        }
        insertEntity(tag)
    }

    @Update
    abstract suspend fun update(tag: TagEntity): Int

    @Delete
    abstract suspend fun delete(tag: TagEntity): Int

    @Query("SELECT * FROM tags WHERE id = :id")
    abstract suspend fun getById(id: String): TagEntity?

    @Query(
        """
        SELECT * FROM tags
        WHERE subject = :subject
        ORDER BY sort_order, name COLLATE NOCASE
        """,
    )
    abstract suspend fun getBySubject(subject: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun link(link: ErrorItemTagEntity): Long

    @Query(
        """
        DELETE FROM error_item_tags
        WHERE error_item_id = :errorItemId AND tag_id = :tagId
        """,
    )
    abstract suspend fun unlink(errorItemId: String, tagId: String): Int

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN error_item_tags ON tags.id = error_item_tags.tag_id
        WHERE error_item_tags.error_item_id = :errorItemId
        ORDER BY tags.sort_order, tags.name COLLATE NOCASE
        """,
    )
    abstract suspend fun getTagsForErrorItem(errorItemId: String): List<TagEntity>

    @Query("SELECT COUNT(*) FROM error_item_tags")
    abstract suspend fun linkCount(): Int
}
