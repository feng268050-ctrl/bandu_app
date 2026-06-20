package com.bandu.tiji.feature.tags

import com.bandu.tiji.core.model.id.TagId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TagsContractTest {
    @Test
    fun `initial state selects mathematics and waits for its tree`() {
        assertThat(TagsUiState()).isEqualTo(
            TagsUiState(
                subjects = StandardSubjects,
                selectedSubject = "数学",
                isLoading = true,
            ),
        )
    }

    @Test
    fun `filter navigation effect keeps the selected tag id`() {
        val tagId = TagId("tag-algebra")

        assertThat(TagsEffect.OpenFilteredErrorItems(tagId).tagId).isEqualTo(tagId)
    }

    @Test
    fun `standard subjects are the required nine subjects in stable order`() {
        assertThat(StandardSubjects).containsExactly(
            "数学",
            "物理",
            "化学",
            "生物",
            "英语",
            "语文",
            "历史",
            "地理",
            "政治",
        ).inOrder()
    }
}
