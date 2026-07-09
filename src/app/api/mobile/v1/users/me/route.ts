import { getMobileUser, mapMobileUser, mobileError, mobileOk } from "@/lib/mobile-api";

export const runtime = "nodejs";

export async function GET(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    return mobileOk(mapMobileUser(user));
}
