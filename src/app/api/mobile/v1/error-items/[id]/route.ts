import { prisma } from "@/lib/prisma";
import {
    getMobileUser,
    mapMobileErrorItemDetail,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

export async function GET(
    req: Request,
    { params }: { params: Promise<{ id: string }> }
) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const { id } = await params;
    const item = await prisma.errorItem.findUnique({
        where: { id },
        include: {
            subject: true,
            tags: true,
        },
    });

    if (!item) {
        return mobileError(404, "NOT_FOUND", "Item not found");
    }

    if (item.userId !== user.id) {
        return mobileError(403, "FORBIDDEN", "Not authorized to access this item");
    }

    return mobileOk(mapMobileErrorItemDetail(item));
}
