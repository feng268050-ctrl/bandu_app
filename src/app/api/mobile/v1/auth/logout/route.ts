import {
    getMobileUser,
    hashMobileRefreshToken,
    mobileOk,
    mobileRefreshTokens,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

export async function POST(req: Request) {
    const body = await req.json().catch(() => null);
    const refreshToken =
        body && typeof body.refreshToken === "string" ? body.refreshToken : null;
    const user = await getMobileUser(req);

    if (refreshToken) {
        await mobileRefreshTokens().updateMany({
            where: {
                tokenHash: hashMobileRefreshToken(refreshToken),
                revokedAt: null,
            },
            data: { revokedAt: new Date() },
        });
    } else if (user) {
        await mobileRefreshTokens().updateMany({
            where: {
                userId: user.id,
                revokedAt: null,
            },
            data: { revokedAt: new Date() },
        });
    }

    return mobileOk({ success: true });
}
