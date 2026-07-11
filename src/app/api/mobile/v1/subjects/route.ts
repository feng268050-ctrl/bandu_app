import { z } from "zod";
import { prisma } from "@/lib/prisma";
import {
    getMobileUser,
    mapMobileSubject,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

const subjectSchema = z.object({
    name: z.string().min(1).max(40),
});

export async function GET(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const subjects = await prisma.subject.findMany({
        where: { userId: user.id },
        orderBy: [{ updatedAt: "desc" }, { name: "asc" }],
    });

    return mobileOk(subjects.map(mapMobileSubject));
}

export async function POST(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const parsed = subjectSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "科目名称格式不正确", parsed.error.flatten());
    }

    const name = parsed.data.name.trim();
    const subject = await prisma.subject.upsert({
        where: {
            name_userId: {
                name,
                userId: user.id,
            },
        },
        update: {},
        create: {
            name,
            userId: user.id,
        },
    });

    return mobileOk(mapMobileSubject(subject));
}
