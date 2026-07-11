import { z } from "zod";
import { getAIService } from "@/lib/ai";
import type { DifficultyLevel } from "@/lib/ai/types";
import {
    getMobileUser,
    mapMobilePracticeQuestion,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";
import { prisma } from "@/lib/prisma";

export const runtime = "nodejs";

const generatePracticeSchema = z.object({
    errorItemId: z.string().min(1),
    language: z.enum(["zh", "en"]).optional().default("zh"),
    difficulty: z.enum(["easy", "medium", "hard", "harder"]).optional().default("medium"),
});

export async function POST(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const parsed = generatePracticeSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "练习参数格式不正确", parsed.error.flatten());
    }

    const item = await prisma.errorItem.findFirst({
        where: {
            id: parsed.data.errorItemId,
            userId: user.id,
        },
        include: { subject: true, tags: true },
    });
    if (!item) {
        return mobileError(404, "NOT_FOUND", "Item not found");
    }

    const tags = item.tags.length > 0 ? item.tags.map((tag) => tag.name) : parseTagNames(item.knowledgePoints);
    const question = await getAIService().generateSimilarQuestion(
        item.questionText || "",
        tags,
        parsed.data.language,
        parsed.data.difficulty as DifficultyLevel,
        item.gradeSemester
    );

    if (item.subject?.name) {
        question.subject = item.subject.name as typeof question.subject;
    }

    return mobileOk(mapMobilePracticeQuestion(question));
}

function parseTagNames(value: string | null) {
    if (!value) {
        return [];
    }

    try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed) ? parsed.map((item) => String(item)) : [];
    } catch {
        return [];
    }
}
