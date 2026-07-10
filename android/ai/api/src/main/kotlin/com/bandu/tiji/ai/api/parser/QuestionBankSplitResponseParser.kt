package com.bandu.tiji.ai.api.parser

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.SplitQuestionBankPage
import com.bandu.tiji.ai.api.model.SplitQuestionBankQuestion
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.questionbank.BankQuestionType

class QuestionBankSplitResponseParser {
    fun parsePage(text: String): SplitQuestionBankPage {
        if (!text.contains("<questions>") && !text.contains("<question>")) {
            throw AiError.InvalidResponse("question_bank_split.missing_questions")
        }
        val questions = QUESTION_BLOCK.findAll(text)
            .mapNotNull { match -> parseQuestion(match.groupValues[1]) }
            .toList()
        return SplitQuestionBankPage(questions)
    }

    private fun parseQuestion(block: String): SplitQuestionBankQuestion? {
        val stem = TagExtractor.extractTag(block, "stem")?.trim().orEmpty()
        if (stem.isBlank()) return null
        return SplitQuestionBankQuestion(
            stem = stem,
            options = parseOptions(TagExtractor.extractTag(block, "options").orEmpty()),
            answer = TagExtractor.extractTag(block, "answer").blankToNull(),
            analysis = TagExtractor.extractTag(block, "analysis").blankToNull(),
            questionType = parseQuestionType(TagExtractor.extractTag(block, "type")),
            difficulty = parseDifficulty(TagExtractor.extractTag(block, "difficulty")),
            tags = parseTags(TagExtractor.extractTag(block, "tags").orEmpty()),
            sourceText = TagExtractor.extractTag(block, "source_text").blankToNull(),
        )
    }

    private fun parseQuestionType(raw: String?): BankQuestionType =
        enumValueOrDefault(raw, BankQuestionType.UNKNOWN)

    private fun parseDifficulty(raw: String?): ExerciseDifficulty =
        enumValueOrDefault(raw, ExerciseDifficulty.MEDIUM)

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        val normalized = raw?.trim()?.uppercase().orEmpty()
        return enumValues<T>().firstOrNull { it.name == normalized } ?: default
    }

    private fun parseOptions(raw: String): List<String> =
        raw.lines()
            .map { it.trim().trimStart('-', '*', '·').trim() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun parseTags(raw: String): List<String> =
        raw.lines()
            .flatMap { line -> line.split(TAG_DELIMITER) }
            .map { it.trim().trimStart('-', '*', '·').trim() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun String?.blankToNull(): String? =
        this?.trim()?.ifBlank { null }

    private companion object {
        val QUESTION_BLOCK = Regex(
            pattern = """<question>(.*?)</question>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val TAG_DELIMITER = Regex("""[,，；;]""")
    }
}
