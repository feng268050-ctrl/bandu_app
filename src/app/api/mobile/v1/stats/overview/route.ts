import { format, subDays } from "date-fns";
import { getMobileUser, mobileError, mobileOk } from "@/lib/mobile-api";
import { prisma } from "@/lib/prisma";

export const runtime = "nodejs";

export async function GET(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const [totalErrors, masteredCount, items, totalPractice, correctPractice] =
        await Promise.all([
            prisma.errorItem.count({ where: { userId: user.id } }),
            prisma.errorItem.count({
                where: {
                    userId: user.id,
                    masteryLevel: { gt: 0 },
                },
            }),
            prisma.errorItem.findMany({
                where: { userId: user.id },
                select: {
                    createdAt: true,
                    subject: { select: { name: true } },
                },
            }),
            prisma.practiceRecord.count({ where: { userId: user.id } }),
            prisma.practiceRecord.count({
                where: {
                    userId: user.id,
                    isCorrect: true,
                },
            }),
        ]);

    const subjectCounts = new Map<string, number>();
    for (const item of items) {
        const name = item.subject?.name || "未分类";
        subjectCounts.set(name, (subjectCounts.get(name) || 0) + 1);
    }

    const recentActivity = [];
    for (let index = 6; index >= 0; index--) {
        const day = subDays(new Date(), index);
        const start = new Date(day);
        start.setHours(0, 0, 0, 0);
        const end = new Date(day);
        end.setHours(23, 59, 59, 999);
        recentActivity.push({
            date: format(day, "MM-dd"),
            count: items.filter(
                (item) => item.createdAt >= start && item.createdAt <= end
            ).length,
        });
    }

    return mobileOk({
        totalErrors,
        masteredCount,
        masteryRate: totalErrors > 0 ? masteredCount / totalErrors : 0,
        practiceTotal: totalPractice,
        practiceCorrect: correctPractice,
        practiceAccuracy: totalPractice > 0 ? correctPractice / totalPractice : 0,
        subjectStats: Array.from(subjectCounts.entries()).map(([name, value]) => ({
            name,
            value,
        })),
        recentActivity,
    });
}
