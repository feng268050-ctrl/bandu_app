import { z } from "zod";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mapMobileUser, mobileError, mobileOk } from "@/lib/mobile-api";

export const runtime = "nodejs";

export async function GET(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    return mobileOk(mapMobileUser(user));
}

const updateProfileSchema = z
    .object({
        name: z.string().max(40).optional().nullable(),
        educationStage: z
            .enum(["primary", "junior_high", "senior_high", "university"])
            .optional()
            .nullable(),
        enrollmentYear: z.number().int().min(1900).max(3000).optional().nullable(),
    })
    .refine((value) => Object.keys(value).length > 0, {
        message: "At least one profile field is required",
    });

export async function PATCH(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const parsed = updateProfileSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "学生资料格式不正确", parsed.error.flatten());
    }

    const updatedUser = await prisma.user.update({
        where: { id: user.id },
        data: {
            name:
                parsed.data.name === undefined
                    ? undefined
                    : parsed.data.name?.trim() || null,
            educationStage: parsed.data.educationStage,
            enrollmentYear: parsed.data.enrollmentYear,
        },
    });

    return mobileOk(mapMobileUser(updatedUser));
}
