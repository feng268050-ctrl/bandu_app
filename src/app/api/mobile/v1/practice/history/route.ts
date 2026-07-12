import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileError, mobileOk } from "@/lib/mobile-api";

export const runtime = "nodejs";

export async function GET(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const url = new URL(req.url);
    const requestedLimit = Number.parseInt(url.searchParams.get("limit") || "20", 10);
    const limit = Number.isFinite(requestedLimit)
        ? Math.min(Math.max(requestedLimit, 1), 100)
        : 20;
    const records = await prisma.practiceRecord.findMany({
        where: { userId: user.id },
        orderBy: { createdAt: "desc" },
        take: limit,
    });

    return mobileOk(
        records.map((record) => ({
            id: record.id,
            subject: record.subject,
            difficulty: record.difficulty,
            isCorrect: record.isCorrect,
            createdAt: record.createdAt.toISOString(),
        }))
    );
}
