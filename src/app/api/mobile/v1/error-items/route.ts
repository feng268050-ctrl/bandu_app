import type { Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import {
    getMobileUser,
    mapMobileErrorItemSummary,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

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
