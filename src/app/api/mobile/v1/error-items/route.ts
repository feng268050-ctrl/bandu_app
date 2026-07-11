import type { Prisma } from "@prisma/client";
import { z } from "zod";
import { calculateGrade } from "@/lib/grade-calculator";
import { inferSubjectFromName } from "@/lib/knowledge-tags";
import { normalizeMistakeStatusForSave } from "@/lib/mistake-status";
import { prisma } from "@/lib/prisma";
import {
    getMobileUser,
    mapMobileErrorItemDetail,
    mapMobileErrorItemSummary,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";
import { findParentTagIdForGrade } from "@/lib/tag-recognition";

export const runtime = "nodejs";

const saveErrorItemSchema = z.object({
    questionText: z.string().optional().nullable(),
    answer: z.string().optional().nullable(),
    answerText: z.string().optional().nullable(),
    analysis: z.string().optional().nullable(),
    wrongAnswerText: z.string().optional().nullable(),
    mistakeAnalysis: z.string().optional().nullable(),
    mistakeStatus: z.string().optional().nullable(),
    tags: z.array(z.string()).optional(),
    knowledgePoints: z.array(z.string()).optional(),
    originalImageUrl: z.string().optional().nullable(),
    subjectId: z.string().optional().nullable(),
    subjectName: z.string().optional().nullable(),
    gradeSemester: z.string().optional().nullable(),
    paperLevel: z.string().optional().nullable(),
    geogebraCommands: z.string().optional().nullable(),
    source: z.string().optional().nullable(),
    userNotes: z.string().optional().nullable(),
});

export async function GET(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const { searchParams } = new URL(req.url);
    const page = Math.max(1, Number.parseInt(searchParams.get("page") || "1", 10));
    const pageSize = Math.min(
        100,
        Math.max(1, Number.parseInt(searchParams.get("pageSize") || "30", 10))
    );
    const subjectId = searchParams.get("subjectId");
    const query = searchParams.get("query");

    const where: Prisma.ErrorItemWhereInput = { userId: user.id };
    if (subjectId) {
        where.subjectId = subjectId;
    }
    if (query) {
        where.OR = [
            { questionText: { contains: query } },
            { answerText: { contains: query } },
            { analysis: { contains: query } },
            { knowledgePoints: { contains: query } },
        ];
    }

    const [total, items] = await Promise.all([
        prisma.errorItem.count({ where }),
        prisma.errorItem.findMany({
            where,
            orderBy: { updatedAt: "desc" },
            include: {
                subject: true,
                tags: true,
            },
            skip: (page - 1) * pageSize,
            take: pageSize,
        }),
    ]);

    return mobileOk(
        items.map(mapMobileErrorItemSummary),
        {
            pagination: {
                page,
                pageSize,
                total,
                totalPages: Math.ceil(total / pageSize),
            },
        }
    );
}

export async function POST(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const parsed = saveErrorItemSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "错题数据格式不正确", parsed.error.flatten());
    }

    const body = parsed.data;
    const questionText = body.questionText?.trim() || null;
    const answerText = (body.answerText ?? body.answer)?.trim() || null;
    const analysis = body.analysis?.trim() || null;
    const tagNames = normalizeTagNames(body.tags ?? body.knowledgePoints ?? []);
    const subjectId = await resolveSubjectId({
        userId: user.id,
        subjectId: body.subjectId,
        subjectName: body.subjectName,
    });
    const gradeSemester =
        body.gradeSemester?.trim() ||
        (user.educationStage && user.enrollmentYear
            ? calculateGrade(user.educationStage, user.enrollmentYear)
            : null);
    const tagConnections = await resolveTagConnections({
        userId: user.id,
        subjectId,
        gradeSemester,
        tagNames,
    });

    const item = await prisma.errorItem.create({
        data: {
            userId: user.id,
            subjectId: subjectId ?? undefined,
            originalImageUrl: body.originalImageUrl ?? "",
            ocrText: questionText,
            questionText,
            answerText,
            analysis,
            wrongAnswerText: body.wrongAnswerText || null,
            mistakeAnalysis: body.mistakeAnalysis || null,
            mistakeStatus: normalizeMistakeStatusForSave(
                body.mistakeStatus,
                body.wrongAnswerText
            ),
            knowledgePoints: JSON.stringify(tagNames),
            source: body.source || "mobile",
            userNotes: body.userNotes || null,
            gradeSemester,
            paperLevel: body.paperLevel || null,
            geogebraCommands: body.geogebraCommands || null,
            masteryLevel: 0,
            tags: {
                connect: tagConnections,
            },
        },
        include: {
            subject: true,
            tags: true,
        },
    });

    return mobileOk(mapMobileErrorItemDetail(item));
}

function normalizeTagNames(values: string[]) {
    return Array.from(
        new Set(
            values
                .map((value) => value.trim())
                .filter((value) => value.length > 0)
        )
    ).slice(0, 12);
}

async function resolveSubjectId(args: {
    userId: string;
    subjectId?: string | null;
    subjectName?: string | null;
}) {
    if (args.subjectId) {
        const subject = await prisma.subject.findFirst({
            where: {
                id: args.subjectId,
                userId: args.userId,
            },
            select: { id: true },
        });
        return subject?.id ?? null;
    }

    const subjectName = args.subjectName?.trim();
    if (!subjectName) {
        return null;
    }

    const subject = await prisma.subject.upsert({
        where: {
            name_userId: {
                name: subjectName,
                userId: args.userId,
            },
        },
        update: {},
        create: {
            name: subjectName,
            userId: args.userId,
        },
        select: { id: true },
    });
    return subject.id;
}

async function resolveTagConnections(args: {
    userId: string;
    subjectId: string | null;
    gradeSemester: string | null;
    tagNames: string[];
}) {
    if (args.tagNames.length === 0) {
        return [];
    }

    const subject = args.subjectId
        ? await prisma.subject.findFirst({
              where: {
                  id: args.subjectId,
                  userId: args.userId,
              },
              select: { name: true },
          })
        : null;
    const subjectKey = inferSubjectFromName(subject?.name ?? null) || "other";
    const connections: { id: string }[] = [];

    for (const tagName of args.tagNames) {
        let tag = await prisma.knowledgeTag.findFirst({
            where: {
                name: tagName,
                subject: subjectKey,
                OR: [{ isSystem: true }, { userId: args.userId }],
            },
            select: { id: true },
        });

        if (!tag) {
            tag = await prisma.knowledgeTag.create({
                data: {
                    name: tagName,
                    subject: subjectKey,
                    isSystem: false,
                    userId: args.userId,
                    parentId: await findParentTagIdForGrade(
                        args.gradeSemester,
                        subjectKey
                    ),
                },
                select: { id: true },
            });
        }

        connections.push({ id: tag.id });
    }

    return connections;
}
