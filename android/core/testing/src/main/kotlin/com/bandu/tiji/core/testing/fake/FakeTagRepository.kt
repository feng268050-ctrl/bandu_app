package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.tag.CreateTagInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTagRepository(
    initialTags: List<TagNode> = emptyList(),
) : TagRepository {
    private val tags = MutableStateFlow(initialTags.toRecords())
    private var nextId = tags.value.size + 1

    val failures = FailureInjector()
    val createdTags = mutableListOf<CreateTagInput>()
    val renamedTags = mutableListOf<Pair<TagId, String>>()
    val deletedIds = mutableListOf<TagId>()

    override fun observeTree(subject: String?): Flow<List<TagNode>> =
        tags.map { records ->
            records
                .filterValues { subject == null || it.summary.subject == subject }
                .toTree()
        }

    override suspend fun findTag(id: TagId): TagSummary? {
        failures.throwIfQueued()
        return tags.value[id]?.summary
    }

    override suspend fun createCustom(input: CreateTagInput): TagId {
        failures.throwIfQueued()
        createdTags += input
        val id = TagId("tag-${nextId++}")
        tags.value +=
            id to
                TagRecord(
                    summary = TagSummary(id, input.name, input.subject, isSystem = false),
                    parentId = input.parentId,
                    code = null,
                    sortOrder = tags.value.size,
                    linkedErrorItemCount = 0,
                )
        return id
    }

    override suspend fun renameCustom(id: TagId, name: String) {
        failures.throwIfQueued()
        renamedTags += id to name
        tags.value[id]?.let { record ->
            tags.value += id to record.copy(summary = record.summary.copy(name = name))
        }
    }

    override suspend fun deleteCustom(id: TagId) {
        failures.throwIfQueued()
        deletedIds += id
        val descendantIds = tags.value.descendantsOf(id)
        tags.value = tags.value - descendantIds - id
    }

    fun emit(nodes: List<TagNode>) {
        tags.value = nodes.toRecords()
    }

    private data class TagRecord(
        val summary: TagSummary,
        val parentId: TagId?,
        val code: String?,
        val sortOrder: Int,
        val linkedErrorItemCount: Int,
    )

    private companion object {
        fun List<TagNode>.toRecords(): Map<TagId, TagRecord> {
            val records = linkedMapOf<TagId, TagRecord>()

            fun visit(node: TagNode, parentId: TagId?) {
                records[node.tag.id] =
                    TagRecord(
                        summary = node.tag,
                        parentId = parentId,
                        code = node.code,
                        sortOrder = node.sortOrder,
                        linkedErrorItemCount = node.linkedErrorItemCount,
                    )
                node.children.forEach { visit(it, node.tag.id) }
            }

            forEach { visit(it, null) }
            return records
        }

        fun Map<TagId, TagRecord>.toTree(): List<TagNode> {
            fun node(record: TagRecord): TagNode =
                TagNode(
                    tag = record.summary,
                    code = record.code,
                    sortOrder = record.sortOrder,
                    linkedErrorItemCount = record.linkedErrorItemCount,
                    children =
                        values
                            .filter { it.parentId == record.summary.id }
                            .sortedBy(TagRecord::sortOrder)
                            .map(::node),
                )

            return values
                .filter { it.parentId == null || it.parentId !in keys }
                .sortedBy(TagRecord::sortOrder)
                .map(::node)
        }

        fun Map<TagId, TagRecord>.descendantsOf(parentId: TagId): Set<TagId> {
            val direct = filterValues { it.parentId == parentId }.keys
            return direct + direct.flatMap { descendantsOf(it) }
        }
    }
}
