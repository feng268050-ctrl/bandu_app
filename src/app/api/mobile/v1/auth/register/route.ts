import { hash } from "bcryptjs";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { issueMobileSession, mobileError, mobileOk } from "@/lib/mobile-api";

export const runtime = "nodejs";

const registerSchema = z.object({
    email: z.string().regex(/^[^\s@]+@[^\s@]+\.[^\s@]+$/, "Invalid email format"),
    password: z.string().min(6),
    name: z.string().optional().nullable(),
    educationStage: z.string().optional().nullable(),
    enrollmentYear: z.number().int().min(1900).max(3000).optional().nullable(),
});

export async function POST(req: Request) {
    const parsed = registerSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "注册信息格式不正确", parsed.error.flatten());
    }

    const email = parsed.data.email.trim().toLowerCase();
    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) {
        return mobileError(409, "EMAIL_EXISTS", "该邮箱已注册");
    }

    const user = await prisma.user.create({
        data: {
            email,
            password: await hash(parsed.data.password, 10),
            name: parsed.data.name?.trim() || null,
            educationStage: parsed.data.educationStage || null,
            enrollmentYear: parsed.data.enrollmentYear ?? null,
        },
    });

    return mobileOk(await issueMobileSession(user));
}
