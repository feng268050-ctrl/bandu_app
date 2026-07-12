import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileError, mobileOk } from "@/lib/mobile-api";

export const runtime = "nodejs";

const submitPracticeSchema = z.object({
    subject: z.string().max(80).optional().nullable(),
    difficulty: z.enum(["easy", "medium", "hard", "harder"]).optional().nullable(),
    isCorrect: z.boolean(),
});

export async function POST(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const parsed = submitPracticeSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "练习记录格式不正确", parsed.error.flatten());
    }

    const record = await prisma.practiceRecord.create({
        data: {
            userId: user.id,
            subject: parsed.data.subject?.trim() || null,
            difficulty: parsed.data.difficulty || null,
            isCorrect: parsed.data.isCorrect,
        },
    });

    return mobileOk({
        id: record.id,
        subject: record.subject,
        difficulty: record.difficulty,
        isCorrect: record.isCorrect,
        createdAt: record.createdAt.toISOString(),
    });
}
