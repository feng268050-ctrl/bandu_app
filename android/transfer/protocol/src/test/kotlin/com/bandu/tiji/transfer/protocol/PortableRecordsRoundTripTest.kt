package com.bandu.tiji.transfer.protocol

import com.bandu.tiji.transfer.protocol.proto.CollectionRecord
import com.bandu.tiji.transfer.protocol.proto.ErrorItemRecord
import com.bandu.tiji.transfer.protocol.proto.ErrorItemTagRecord
import com.bandu.tiji.transfer.protocol.proto.ExerciseDifficultyRecord
import com.bandu.tiji.transfer.protocol.proto.ExerciseRecord
import com.bandu.tiji.transfer.protocol.proto.GradeResultRecord
import com.bandu.tiji.transfer.protocol.proto.MasteryLevelRecord
import com.bandu.tiji.transfer.protocol.proto.MistakeStatusRecord
import com.bandu.tiji.transfer.protocol.proto.PaperLevelRecord
import com.bandu.tiji.transfer.protocol.proto.PortablePreferencesRecord
import com.bandu.tiji.transfer.protocol.proto.PortableProviderType
import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import com.bandu.tiji.transfer.protocol.proto.TagRecord
import com.bandu.tiji.transfer.protocol.proto.TutorMessageRecord
import com.bandu.tiji.transfer.protocol.proto.TutorMessageRoleRecord
import com.bandu.tiji.transfer.protocol.proto.TutorMessageStatusRecord
import com.bandu.tiji.transfer.protocol.proto.TutorSessionRecord
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Test

class PortableRecordsRoundTripTest {
    @Test
    fun `all portable records round trip without device secrets`() {
        val hash = ByteString.copyFrom(ByteArray(32) { 7 })
        val records = listOf(
            PortableRecord.newBuilder().setCollection(
                CollectionRecord.newBuilder().setId("c").setName("期中").setCreatedAtEpochMs(1).setUpdatedAtEpochMs(2),
            ).build(),
            PortableRecord.newBuilder().setErrorItem(
                ErrorItemRecord.newBuilder()
                    .setId("e")
                    .setCollectionId("c")
                    .setImageRelativePath("images/e.jpg")
                    .setImageSha256(hash)
                    .setQuestionText("1+1")
                    .setAnswerText("2")
                    .setAnalysis("加法")
                    .setMistakeStatus(MistakeStatusRecord.MISTAKE_STATUS_RECORD_WRONG_ATTEMPT)
                    .setPaperLevel(PaperLevelRecord.PAPER_LEVEL_RECORD_A)
                    .setMasteryLevel(MasteryLevelRecord.MASTERY_LEVEL_RECORD_NEW),
            ).build(),
            PortableRecord.newBuilder().setTag(
                TagRecord.newBuilder().setId("t").setName("加法").setSubject("数学").setIsSystem(true),
            ).build(),
            PortableRecord.newBuilder().setErrorItemTag(
                ErrorItemTagRecord.newBuilder().setErrorItemId("e").setTagId("t"),
            ).build(),
            PortableRecord.newBuilder().setTutorSession(
                TutorSessionRecord.newBuilder().setId("s").setTitle("讲解").setErrorItemId("e"),
            ).build(),
            PortableRecord.newBuilder().setTutorMessage(
                TutorMessageRecord.newBuilder()
                    .setId("m")
                    .setSessionId("s")
                    .setRole(TutorMessageRoleRecord.TUTOR_MESSAGE_ROLE_RECORD_ASSISTANT)
                    .setStatus(TutorMessageStatusRecord.TUTOR_MESSAGE_STATUS_RECORD_COMPLETE)
                    .setContent("答案是 2"),
            ).build(),
            PortableRecord.newBuilder().setExercise(
                ExerciseRecord.newBuilder()
                    .setId("x")
                    .setSessionId("s")
                    .setDifficulty(ExerciseDifficultyRecord.EXERCISE_DIFFICULTY_RECORD_EASY)
                    .setQuestionText("2+2")
                    .setExpectedAnswer("4")
                    .setAiResult(GradeResultRecord.GRADE_RESULT_RECORD_CORRECT),
            ).build(),
            PortableRecord.newBuilder().setPortablePreferences(
                PortablePreferencesRecord.newBuilder()
                    .setStudentNickname("小明")
                    .setProviderType(PortableProviderType.PORTABLE_PROVIDER_TYPE_GEMINI)
                    .setProviderDisplayName("Gemini")
                    .setProviderBaseUrl("https://generativelanguage.googleapis.com")
                    .setAnalysisModel("gemini")
                    .setTutorModel("gemini")
                    .setPromptSchemaVersion(1),
            ).build(),
        )

        records.forEach { record ->
            assertThat(PortableRecord.parseFrom(record.toByteArray())).isEqualTo(record)
        }
    }
}
