import { z } from "zod";
import { prisma } from "@/lib/prisma";
import {
    getMobileUser,
    mapMobileErrorItemDetail,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

const patchErrorItemSchema = z.object({
    questionText: z.string().optional().nullable(),
    answer: z.string().optional().nullable(),
    answerText: z.string().optional().nullable(),
    analysis: z.string().optional().nullable(),
    wrongAnswerText: z.string().optional().nullable(),
    mistakeAnalysis: z.string().optional().nullable(),
    mistakeStatus: z.string().optional().nullable(),
    masteryLevel: z.number().int().min(0).max(2).optional(),
    gradeSemester: z.string().optional().nullable(),
    paperLevel: z.string().optional().nullable(),
    userNotes: z.string().optional().nullable(),
});

export async function GET(
    req: Request,
    { params }: { params: Promise<{ id: string }> }
) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const { id } = await params;
    const item = await prisma.errorItem.findUnique({
        where: { id },
        include: {
            subject: true,
            tags: true,
        },
    });

    if (!item) {
        return mobileError(404, "NOT_FOUND", "Item not found");
    }

    if (item.userId !== user.id) {
        return mobileError(403, "FORBIDDEN", "Not authorized to access this item");
    }

    return mobileOk(mapMobileErrorItemDetail(item));
}

export async function PATCH(
    req: Request,
    { params }: { params: Promise<{ id: string }> }
) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const { id } = await params;
    const existing = await prisma.errorItem.findUnique({
        where: { id },
        select: { userId: true },
    });

    if (!existing) {
        return mobileError(404, "NOT_FOUND", "Item not found");
    }
    if (existing.userId !== user.id) {
        return mobileError(403, "FORBIDDEN", "Not authorized to update this item");
    }

    const parsed = patchErrorItemSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "错题数据格式不正确", parsed.error.flatten());
    }

    const body = parsed.data;
    const item = await prisma.errorItem.update({
        where: { id },
        data: {
            questionText: body.questionText === undefined ? undefined : body.questionText,
            answerText:
                body.answerText === undefined && body.answer === undefined
                    ? undefined
                    : body.answerText ?? body.answer,
            analysis: body.analysis === undefined ? undefined : body.analysis,
            wrongAnswerText:
                body.wrongAnswerText === undefined ? undefined : body.wrongAnswerText,
            mistakeAnalysis:
                body.mistakeAnalysis === undefined ? undefined : body.mistakeAnalysis,
            mistakeStatus:
                body.mistakeStatus === undefined ? undefined : body.mistakeStatus,
            masteryLevel: body.masteryLevel,
            gradeSemester:
                body.gradeSemester === undefined ? undefined : body.gradeSemester,
            paperLevel: body.paperLevel === undefined ? undefined : body.paperLevel,
            userNotes: body.userNotes === undefined ? undefined : body.userNotes,
        },
        include: {
            subject: true,
            tags: true,
        },
    });

    return mobileOk(mapMobileErrorItemDetail(item));
}

export async function DELETE(
    req: Request,
    { params }: { params: Promise<{ id: string }> }
) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const { id } = await params;
    const existing = await prisma.errorItem.findUnique({
        where: { id },
        select: { userId: true },
    });

    if (!existing) {
        return mobileError(404, "NOT_FOUND", "Item not found");
    }
    if (existing.userId !== user.id) {
        return mobileError(403, "FORBIDDEN", "Not authorized to delete this item");
    }

    await prisma.errorItem.delete({ where: { id } });
    return mobileOk({ success: true });
}
