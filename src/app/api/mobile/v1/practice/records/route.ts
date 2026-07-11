import { z } from "zod";
import { getMobileUser, mobileError, mobileOk } from "@/lib/mobile-api";
import { prisma } from "@/lib/prisma";

export const runtime = "nodejs";

const recordPracticeSchema = z.object({
    subject: z.string().optional().nullable(),
    difficulty: z.enum(["easy", "medium", "hard", "harder"]).optional().nullable(),
    isCorrect: z.boolean().optional().nullable(),
});

export async function POST(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const parsed = recordPracticeSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "练习记录格式不正确", parsed.error.flatten());
    }

    const record = await prisma.practiceRecord.create({
        data: {
            userId: user.id,
            subject: parsed.data.subject || null,
            difficulty: parsed.data.difficulty || null,
            isCorrect: parsed.data.isCorrect ?? null,
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
