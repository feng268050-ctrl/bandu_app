import { z } from "zod";
import { prisma } from "@/lib/prisma";
import {
    getMobileUser,
    mapMobileKnowledgeTag,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

type MobileTagTreeNode = ReturnType<typeof mapMobileKnowledgeTag> & {
    children: MobileTagTreeNode[];
};

const createTagSchema = z.object({
    name: z.string().min(1).max(80),
    subject: z.string().min(1).max(40),
    parentId: z.string().optional().nullable(),
});

export async function GET(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const { searchParams } = new URL(req.url);
    const subject = searchParams.get("subject");
    const flat = searchParams.get("flat") === "true";

    if (!subject) {
        return mobileError(400, "SUBJECT_REQUIRED", "subject is required");
    }

    const tags = await prisma.knowledgeTag.findMany({
        where: {
            subject,
            OR: [{ isSystem: true }, { userId: user.id }],
        },
        orderBy: [{ order: "asc" }, { name: "asc" }],
    });

    if (flat) {
        return mobileOk(tags.map(mapMobileKnowledgeTag));
    }

    return mobileOk(buildTagTree(tags.map(mapMobileKnowledgeTag)));
}

export async function POST(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const parsed = createTagSchema.safeParse(await req.json().catch(() => null));
    if (!parsed.success) {
        return mobileError(400, "INVALID_INPUT", "标签格式不正确", parsed.error.flatten());
    }

    const name = parsed.data.name.trim();
    const existing = await prisma.knowledgeTag.findFirst({
        where: {
            name,
            subject: parsed.data.subject,
            userId: user.id,
            parentId: parsed.data.parentId ?? null,
        },
    });
    if (existing) {
        return mobileOk(mapMobileKnowledgeTag(existing));
    }

    const tag = await prisma.knowledgeTag.create({
        data: {
            name,
            subject: parsed.data.subject,
            parentId: parsed.data.parentId ?? null,
            isSystem: false,
            userId: user.id,
        },
    });

    return mobileOk(mapMobileKnowledgeTag(tag));
}

function buildTagTree(tags: ReturnType<typeof mapMobileKnowledgeTag>[]) {
    const nodes = new Map<string, MobileTagTreeNode>();
    const roots: MobileTagTreeNode[] = [];

    for (const tag of tags) {
        nodes.set(tag.id, {
            ...tag,
            children: [],
        });
    }

    for (const tag of tags) {
        const node = nodes.get(tag.id);
        if (!node) {
            continue;
        }

        if (tag.parentId && nodes.has(tag.parentId)) {
            nodes.get(tag.parentId)!.children.push(node);
        } else {
            roots.push(node);
        }
    }

    return roots;
}
