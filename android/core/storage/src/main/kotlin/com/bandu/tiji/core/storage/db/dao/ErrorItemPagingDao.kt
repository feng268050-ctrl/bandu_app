package com.bandu.tiji.core.storage.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.bandu.tiji.core.storage.db.projection.ErrorItemSummaryProjection

@Dao
interface ErrorItemPagingDao {
    @Query(
        """
        SELECT
            error_items.id,
            error_items.collection_id,
            collections.name AS collection_name,
            error_items.image_path,
            error_items.question_text,
            error_items.mastery_level,
            error_items.created_at,
            error_items.updated_at
        FROM error_items
        INNER JOIN collections ON collections.id = error_items.collection_id
        WHERE (:collectionId IS NULL OR error_items.collection_id = :collectionId)
          AND (
              :ftsQuery IS NULL OR error_items.rowid IN (
                  SELECT rowid FROM error_item_fts
                  WHERE error_item_fts MATCH :ftsQuery
              )
          )
          AND (
              :masteryLevelCount = 0
              OR error_items.mastery_level IN (:masteryLevels)
          )
          AND (
              :createdAfter IS NULL
              OR error_items.created_at >= :createdAfter
          )
          AND (
              :tagIdCount = 0
              OR (
                  SELECT COUNT(DISTINCT error_item_tags.tag_id)
                  FROM error_item_tags
                  WHERE error_item_tags.error_item_id = error_items.id
                    AND error_item_tags.tag_id IN (:tagIds)
              ) = :tagIdCount
          )
          AND (
              :gradeSemester IS NULL
              OR error_items.grade_semester = :gradeSemester
          )
          AND (
              :paperLevelCount = 0
              OR error_items.paper_level IN (:paperLevels)
          )
        ORDER BY error_items.updated_at DESC, error_items.id
        """,
    )
    fun page(
        collectionId: String?,
        ftsQuery: String?,
        masteryLevels: List<Int>,
        masteryLevelCount: Int,
        createdAfter: Long?,
        tagIds: List<String>,
        tagIdCount: Int,
        gradeSemester: String?,
        paperLevels: List<String>,
        paperLevelCount: Int,
    ): PagingSource<Int, ErrorItemSummaryProjection>
}
