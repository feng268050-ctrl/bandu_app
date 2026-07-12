import { compare } from "bcryptjs";
import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { issueMobileSession, mobileError, mobileOk } from "@/lib/mobile-api";

export const runtime = "nodejs";

const loginSchema = z.object({
    email: z.string().regex(/^[^\s@]+@[^\s@]+$/, "Invalid email format"),
    password: z.string().min(1),
});

export async function POST(req: Request) {
    const parsed = loginSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "邮箱或密码格式不正确", parsed.error.flatten());
    }

    const user = await prisma.user.findUnique({
        where: { email: parsed.data.email.trim().toLowerCase() },
    });

    if (!user || !user.isActive) {
        return mobileError(401, "INVALID_CREDENTIALS", "邮箱或密码不正确");
    }

    const isPasswordValid = await compare(parsed.data.password, user.password);
    if (!isPasswordValid) {
        return mobileError(401, "INVALID_CREDENTIALS", "邮箱或密码不正确");
    }

    return mobileOk(await issueMobileSession(user));
}
