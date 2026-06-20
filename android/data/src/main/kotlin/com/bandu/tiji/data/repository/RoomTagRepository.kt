package com.bandu.tiji.data.repository

import androidx.room.withTransaction
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.bandu.tiji.data.mapper.toDomainSummary
import com.bandu.tiji.data.mapper.toDomainTree
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.tag.CreateTagInput
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomTagRepository @Inject constructor(
    private val database: LearningDatabase,
    private val uuidGenerator: UuidGenerator,
    private val clock: Clock,
) : TagRepository {
    private val tagDao = database.tagDao()

    override fun observeTree(subject: String?): Flow<List<TagNode>> =
        database.invalidationTracker
            .createFlow("tags", "error_item_tags", emitInitialState = true)
            .map {
                database.readTags(subject).toDomainTree(database.readTagLinkCounts())
            }

    override suspend fun findTag(id: TagId): TagSummary? =
        tagDao.getById(id.value)?.toDomainSummary()

    override suspend fun createCustom(input: CreateTagInput): TagId {
        val id = TagId(uuidGenerator.newUuid())
        val now = clock.nowEpochMillis()
        database.withTransaction {
            val parent = input.parentId?.let { parentId ->
                requireNotNull(tagDao.getById(parentId.value)) { "Parent tag does not exist" }
            }
            require(parent == null || parent.subject == input.subject) {
                "Parent tag must use the same subject"
            }
            val siblings = database.readTags(input.subject)
                .filter { it.parentId == input.parentId?.value }
            tagDao.insert(
                TagEntity(
                    id = id.value,
                    name = input.name,
                    subject = input.subject,
                    parentId = input.parentId?.value,
                    sortOrder = (siblings.maxOfOrNull(TagEntity::sortOrder) ?: -1) + 1,
                    code = null,
                    isSystem = false,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        return id
    }

    override suspend fun renameCustom(id: TagId, name: String) {
        database.withTransaction {
            val tag = requireNotNull(tagDao.getById(id.value)) { "Tag does not exist" }
            require(!tag.isSystem) { "System tags are read-only" }
            tagDao.update(tag.copy(name = name, updatedAt = clock.nowEpochMillis()))
        }
    }

    override suspend fun deleteCustom(id: TagId) {
        database.withTransaction {
            val tag = requireNotNull(tagDao.getById(id.value)) { "Tag does not exist" }
            require(!tag.isSystem) { "System tags are read-only" }
            tagDao.delete(tag)
        }
    }

    private fun LearningDatabase.readTags(subject: String?): List<TagEntity> {
        val sql = buildString {
            append("SELECT * FROM tags")
            if (subject != null) append(" WHERE subject = ?")
            append(" ORDER BY sort_order, name COLLATE NOCASE")
        }
        val arguments = if (subject == null) emptyArray() else arrayOf(subject)
        return openHelper.readableDatabase.query(sql, arguments).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        TagEntity(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                            subject = cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                            parentId = cursor.getStringOrNull("parent_id"),
                            sortOrder = cursor.getInt(cursor.getColumnIndexOrThrow("sort_order")),
                            code = cursor.getStringOrNull("code"),
                            isSystem = cursor.getInt(cursor.getColumnIndexOrThrow("is_system")) != 0,
                            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                        ),
                    )
                }
            }
        }
    }

    private fun LearningDatabase.readTagLinkCounts(): Map<String, Int> =
        openHelper.readableDatabase
            .query(
                """
                SELECT tag_id, COUNT(*) AS link_count
                FROM error_item_tags
                GROUP BY tag_id
                """.trimIndent(),
            )
            .use { cursor ->
                buildMap {
                    while (cursor.moveToNext()) {
                        put(
                            cursor.getString(cursor.getColumnIndexOrThrow("tag_id")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("link_count")),
                        )
                    }
                }
            }
}

private fun android.database.Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndexOrThrow(columnName)
    return if (isNull(index)) null else getString(index)
}
