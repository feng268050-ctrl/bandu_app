package com.bandu.tiji.core.testing.fake

import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.model.stats.WrongItemStats
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.ValidationResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeStateRepositoriesTest {
    @Test
    fun `tutor repository creates session and emits appended messages`() = runTest {
        val repository = FakeTutorRepository()
        val errorItemId = ErrorItemId("error-1")
        val sessionId = repository.getOrCreate(errorItemId)

        repository.observeSession(sessionId).test {
            assertThat(awaitItem()?.messages).isEmpty()

            val userMessageId = repository.appendUserMessage(sessionId, "How do I solve it?")
            val withUserMessage = checkNotNull(awaitItem())
            assertThat(withUserMessage.messages.single().id).isEqualTo(userMessageId)

            repository.appendAssistantMessage(sessionId, "Start by isolating x.")
            val withAssistantMessage = checkNotNull(awaitItem())
            assertThat(withAssistantMessage.messages.map { it.content })
                .containsExactly("How do I solve it?", "Start by isolating x.")
                .inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tutor repository emits seeded sessions and consumes failures`() = runTest {
        val session = sessionFixture()
        val repository = FakeTutorRepository(listOf(session))

        repository.observeSessions().test {
            assertThat(awaitItem().single().id).isEqualTo(session.id)
            repository.deleteSession(session.id)
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }

        repository.failures.enqueue(IllegalStateException("session failed"))
        val failure = runCatching { repository.getOrCreate(null) }.exceptionOrNull()
        assertThat(failure).hasMessageThat().isEqualTo("session failed")
    }

    @Test
    fun `stats repository emits controlled state changes`() = runTest {
        val repository = FakeStatsRepository()
        val updated =
            WrongItemStats(
                totalCount = 4,
                masteredCount = 3,
                subjectCounts = mapOf("Math" to 4),
                monthlyNewCounts = emptyList(),
            )

        repository.observeWrongItemStats().test {
            assertThat(awaitItem().totalCount).isEqualTo(0)
            repository.emitWrongItemStats(updated)
            assertThat(awaitItem()).isEqualTo(updated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `profile repository emits update and factory reset`() = runTest {
        val repository = FakeProfileRepository()
        val student = StudentProfile("Ming", "Middle school", 2025)

        repository.observeProfile().test {
            assertThat(awaitItem().nickname).isEmpty()
            repository.updateProfile(student)
            assertThat(awaitItem()).isEqualTo(student)
            repository.factoryReset()
            assertThat(awaitItem()).isEqualTo(FakeProfileRepository.emptyProfile())
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(repository.factoryResetCalls).isEqualTo(1)
    }

    @Test
    fun `AI configuration activates only successful drafts and clears API key`() = runTest {
        val repository = FakeAiConfigurationRepository()
        val draft =
            AiConfigurationDraft(
                providerType = AiProviderType.GEMINI,
                displayName = "Gemini",
                baseUrl = "https://example.com",
                apiKey = "secret",
                analysisModel = "analysis",
                tutorModel = "tutor",
            )

        repository.observeActiveConfiguration().test {
            assertThat(awaitItem()).isNull()

            repository.validationResult = ValidationResult.Failure(listOf("invalid"))
            assertThat(repository.saveAndActivate(draft)).isInstanceOf(ValidationResult.Failure::class.java)
            expectNoEvents()

            repository.validationResult = ValidationResult.Success
            assertThat(repository.saveAndActivate(draft)).isEqualTo(ValidationResult.Success)
            assertThat(awaitItem()?.hasApiKey).isTrue()

            repository.clearApiKey()
            assertThat(awaitItem()?.hasApiKey).isFalse()
            cancelAndIgnoreRemainingEvents()
        }

        repository.savePrompt(PromptType.TUTOR, "template")
        repository.resetPrompt(PromptType.TUTOR)
        assertThat(repository.savedPrompts).doesNotContainKey(PromptType.TUTOR)
        assertThat(repository.resetPrompts).containsExactly(PromptType.TUTOR)
    }
}
