import { z } from "zod";
import {
    hashMobileRefreshToken,
    issueMobileSession,
    mobileError,
    mobileOk,
    mobileRefreshTokens,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

const refreshSchema = z.object({
    refreshToken: z.string().min(1),
});

export async function POST(req: Request) {
    const parsed = refreshSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "刷新凭证无效", parsed.error.flatten());
    }

    const tokenHash = hashMobileRefreshToken(parsed.data.refreshToken);
    const storedToken = await mobileRefreshTokens().findUnique({
        where: { tokenHash },
        include: { user: true },
    });

    if (
        !storedToken ||
        storedToken.revokedAt ||
        storedToken.expiresAt.getTime() <= Date.now() ||
        !storedToken.user?.isActive
    ) {
        return mobileError(401, "SESSION_EXPIRED", "登录状态已失效");
    }

    await mobileRefreshTokens().update({
        where: { id: storedToken.id },
        data: { revokedAt: new Date() },
    });

    return mobileOk(await issueMobileSession(storedToken.user));
}
