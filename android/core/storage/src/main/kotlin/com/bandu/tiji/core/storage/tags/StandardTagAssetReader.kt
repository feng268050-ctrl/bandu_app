package com.bandu.tiji.core.storage.tags

import android.content.Context
import android.util.JsonReader
import com.bandu.tiji.core.storage.db.entity.TagEntity
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class StandardTagAssetReader(
    private val context: Context,
) {
    fun read(assetName: String = ASSET_NAME): List<StandardTagDefinition> {
        context.assets.open(assetName).use { input ->
            JsonReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                return readCatalog(reader)
            }
        }
    }

    private fun readCatalog(reader: JsonReader): List<StandardTagDefinition> {
        var schemaVersion: Int? = null
        var tags: List<StandardTagDefinition>? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "schemaVersion" -> schemaVersion = reader.nextInt()
                "tags" -> tags = readTags(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
            "Unsupported standard tag schema: $schemaVersion"
        }
        return requireNotNull(tags) { "Standard tag asset has no tags" }
    }

    private fun readTags(reader: JsonReader): List<StandardTagDefinition> = buildList {
        reader.beginArray()
        while (reader.hasNext()) {
            add(readTag(reader))
        }
        reader.endArray()
    }

    private fun readTag(reader: JsonReader): StandardTagDefinition {
        var id: String? = null
        var name: String? = null
        var subject: String? = null
        var parentId: String? = null
        var sortOrder: Int? = null
        var code: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "name" -> name = reader.nextString()
                "subject" -> subject = reader.nextString()
                "parentId" -> {
                    parentId = if (reader.peek() == android.util.JsonToken.NULL) {
                        reader.nextNull()
                        null
                    } else {
                        reader.nextString()
                    }
                }
                "sortOrder" -> sortOrder = reader.nextInt()
                "code" -> code = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return StandardTagDefinition(
            id = requireNotNull(id),
            name = requireNotNull(name),
            subject = requireNotNull(subject),
            parentId = parentId,
            sortOrder = requireNotNull(sortOrder),
            code = requireNotNull(code),
        )
    }

    companion object {
        const val ASSET_NAME = "standard_tags_v1.json"
        const val SUPPORTED_SCHEMA_VERSION = 1
    }
}

data class StandardTagDefinition(
    val id: String,
    val name: String,
    val subject: String,
    val parentId: String?,
    val sortOrder: Int,
    val code: String,
) {
    fun toEntity(nowEpochMillis: Long): TagEntity = TagEntity(
        id = id,
        name = name,
        subject = subject,
        parentId = parentId,
        sortOrder = sortOrder,
        code = code,
        isSystem = true,
        createdAt = nowEpochMillis,
        updatedAt = nowEpochMillis,
    )
}
