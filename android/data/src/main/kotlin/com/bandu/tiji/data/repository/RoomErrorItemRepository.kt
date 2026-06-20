package com.bandu.tiji.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.withTransaction
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemTagEntity
import com.bandu.tiji.core.storage.db.projection.ErrorItemSummaryProjection
import com.bandu.tiji.data.image.PendingImageCommitter
import com.bandu.tiji.data.mapper.toDomain
import com.bandu.tiji.data.mapper.toDomainSummary
import com.bandu.tiji.data.mapper.toEntity
import com.bandu.tiji.domain.repository.ErrorItemRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomErrorItemRepository @Inject constructor(
    private val database: LearningDatabase,
    private val uuidGenerator: UuidGenerator,
    private val clock: Clock,
    private val imageCommitter: PendingImageCommitter,
) : ErrorItemRepository {
    private val errorItemDao = database.errorItemDao()
    private val pagingDao = database.errorItemPagingDao()
    private val tagDao = database.tagDao()

    override fun page(query: ErrorItemQuery): PagingSource<Int, ErrorItemSummary> {
        val source = pagingDao.page(
            collectionId = query.collectionId?.value,
            ftsQuery = query.keyword.toFtsQueryOrNull(),
            masteryLevels = query.masteryLevels.map { it.storageValue },
            masteryLevelCount = query.masteryLevels.size,
            createdAfter = query.createdAfterEpochMillis,
            tagIds = query.tagIds.map { it.value },
            tagIdCount = query.tagIds.size,
            gradeSemester = query.gradeSemester,
            paperLevels = query.paperLevels.map { it.name },
            paperLevelCount = query.paperLevels.size,
        )
        return DomainErrorItemPagingSource(source, tagDao::getTagsForErrorItem)
    }

    override fun observe(id: ErrorItemId): Flow<ErrorItem?> =
        database.invalidationTracker
            .createFlow("error_items", "error_item_tags", "tags", emitInitialState = true)
            .map {
                errorItemDao.getById(id.value)?.let { entity ->
                    entity.toDomain(tagDao.getTagsForErrorItem(id.value).map { tag -> tag.toDomainSummary() })
                }
            }

    override suspend fun create(draft: ErrorItemDraft): ErrorItemId {
        val id = ErrorItemId(uuidGenerator.newUuid())
        val imageCommit = imageCommitter.commit(draft.image, id)
        try {
            database.withTransaction {
                errorItemDao.insert(
                    draft.copy(image = imageCommit.image).toEntity(id, clock.nowEpochMillis()),
                )
                draft.tagIds.distinct().forEach { tagId ->
                    tagDao.link(ErrorItemTagEntity(id.value, tagId.value))
                }
            }
        } catch (error: Throwable) {
            imageCommitter.rollback(imageCommit)
            throw error
        }
        return id
    }

    override suspend fun update(id: ErrorItemId, patch: ErrorItemPatch) {
        database.withTransaction {
            val current = errorItemDao.getById(id.value) ?: return@withTransaction
            errorItemDao.update(current.applyPatch(patch, clock.nowEpochMillis()))
            patch.tagIds?.let { tagIds ->
                tagDao.getTagsForErrorItem(id.value).forEach { tag ->
                    tagDao.unlink(id.value, tag.id)
                }
                tagIds.distinct().forEach { tagId ->
                    tagDao.link(ErrorItemTagEntity(id.value, tagId.value))
                }
            }
        }
    }

    override suspend fun delete(ids: Set<ErrorItemId>) {
        database.withTransaction {
            ids.forEach { id ->
                errorItemDao.getById(id.value)?.let { errorItemDao.delete(it) }
            }
        }
    }

    private fun ErrorItemEntity.applyPatch(
        patch: ErrorItemPatch,
        updatedAtEpochMillis: Long,
    ): ErrorItemEntity =
        copy(
            collectionId = patch.collectionId?.value ?: collectionId,
            imagePath = patch.image?.relativePath ?: imagePath,
            imageSha256 = patch.image?.sha256Hex ?: imageSha256,
            imageWidth = patch.image?.width ?: imageWidth,
            imageHeight = patch.image?.height ?: imageHeight,
            questionText = patch.questionText ?: questionText,
            answerText = patch.answerText ?: answerText,
            analysis = patch.analysis ?: analysis,
            wrongAnswerText = patch.wrongAnswerText ?: wrongAnswerText,
            mistakeStatus = patch.mistakeStatus?.name ?: mistakeStatus,
            mistakeAnalysis = patch.mistakeAnalysis ?: mistakeAnalysis,
            subject = patch.subject ?: subject,
            gradeSemester = patch.gradeSemester ?: gradeSemester,
            paperLevel = patch.paperLevel?.name ?: paperLevel,
            notes = patch.notes ?: notes,
            masteryLevel = patch.masteryLevel?.storageValue ?: masteryLevel,
            updatedAt = updatedAtEpochMillis,
        )
}

private class DomainErrorItemPagingSource(
    private val source: PagingSource<Int, ErrorItemSummaryProjection>,
    private val loadTags: suspend (String) -> List<com.bandu.tiji.core.storage.db.entity.TagEntity>,
) : PagingSource<Int, ErrorItemSummary>() {
    init {
        source.registerInvalidatedCallback(::invalidate)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ErrorItemSummary> =
        when (val result = source.load(params)) {
            is LoadResult.Page -> LoadResult.Page(
                data = result.data.map { projection ->
                    projection.toDomain(loadTags(projection.id).map { tag -> tag.toDomainSummary() })
                },
                prevKey = result.prevKey,
                nextKey = result.nextKey,
                itemsBefore = result.itemsBefore,
                itemsAfter = result.itemsAfter,
            )
            is LoadResult.Error -> LoadResult.Error(result.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
        }

    override fun getRefreshKey(state: PagingState<Int, ErrorItemSummary>): Int? =
        state.anchorPosition
}

private fun String.toFtsQueryOrNull(): String? {
    val tokens = trim()
        .split(Regex("\\s+"))
        .map { token ->
            token.filter { character -> character.isLetterOrDigit() || character == '_' }
        }
        .filter(String::isNotBlank)
    if (tokens.isEmpty()) return null
    return tokens.joinToString(separator = " AND ") { token -> "$token*" }
}
