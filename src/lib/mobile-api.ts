import { createHash, createHmac, randomBytes, timingSafeEqual } from "crypto";
import { NextResponse } from "next/server";
import type { ErrorItem, KnowledgeTag, Subject, User } from "@prisma/client";
import { prisma } from "@/lib/prisma";

const ACCESS_TOKEN_TTL_SECONDS = 15 * 60;
const REFRESH_TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000;

type MobileAccessTokenPayload = {
    sub: string;
    type: "access";
    iat: number;
    exp: number;
};

type ErrorItemWithRelations = ErrorItem & {
    subject: Subject | null;
    tags: KnowledgeTag[];
};

type MobileRefreshTokenRecord = {
    id: string;
    userId: string;
    tokenHash: string;
    expiresAt: Date;
    revokedAt: Date | null;
    createdAt: Date;
};

type MobileRefreshTokenWithUser = MobileRefreshTokenRecord & {
    user: User;
};

type MobileRefreshTokenDelegate = {
    create(args: {
        data: {
            userId: string;
            tokenHash: string;
            expiresAt: Date;
        };
    }): Promise<MobileRefreshTokenRecord>;
    findUnique(args: {
        where: { tokenHash: string };
        include: { user: true };
    }): Promise<MobileRefreshTokenWithUser | null>;
    update(args: {
        where: { id: string };
        data: { revokedAt: Date };
    }): Promise<MobileRefreshTokenRecord>;
    updateMany(args: {
        where: {
            tokenHash?: string;
            userId?: string;
            revokedAt: null;
        };
        data: { revokedAt: Date };
    }): Promise<{ count: number }>;
};

export function mobileRefreshTokens() {
    return (prisma as unknown as { mobileRefreshToken: MobileRefreshTokenDelegate })
        .mobileRefreshToken;
}

export function mobileOk<T>(data: T, meta?: Record<string, unknown>) {
    return NextResponse.json({
        data,
        error: null,
        meta: meta ?? null,
    });
}

export function mobileError(
    status: number,
    code: string,
    message: string,
    details?: unknown
) {
    return NextResponse.json(
        {
            data: null,
            error: {
                code,
                message,
                ...(details ? { details } : {}),
            },
            meta: null,
        },
        { status }
    );
}

export function mapMobileUser(user: User) {
    return {
        id: user.id,
        email: user.email,
        name: user.name,
        avatarUrl: null,
        educationStage: user.educationStage,
        enrollmentYear: user.enrollmentYear,
        role: user.role,
    };
}

export function mapMobileSubject(subject: Subject) {
    return {
        id: subject.id,
        name: subject.name,
        updatedAt: subject.updatedAt.toISOString(),
    };
}

export function mapMobileKnowledgeTag(tag: KnowledgeTag) {
    return {
        id: tag.id,
        name: tag.name,
        subject: tag.subject,
        parentId: tag.parentId,
        code: tag.code,
        isSystem: tag.isSystem,
        updatedAt: tag.updatedAt.toISOString(),
    };
}

export async function issueMobileSession(user: User) {
    const accessToken = signAccessToken(user.id);
    const refreshToken = randomBytes(48).toString("base64url");

    await mobileRefreshTokens().create({
        data: {
            userId: user.id,
            tokenHash: hashMobileRefreshToken(refreshToken),
            expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL_MS),
        },
    });

    return {
        accessToken,
        refreshToken,
        user: mapMobileUser(user),
    };
}

export function hashMobileRefreshToken(token: string) {
    return createHash("sha256").update(token).digest("hex");
}

export async function getMobileUser(req: Request): Promise<User | null> {
    const token = readBearerToken(req);
    if (!token) {
        return null;
    }

    const payload = verifyAccessToken(token);
    if (!payload) {
        return null;
    }

    const user = await prisma.user.findUnique({
        where: { id: payload.sub },
    });

    if (!user || !user.isActive) {
        return null;
    }

    return user;
}

export function mapMobileErrorItemSummary(item: ErrorItemWithRelations) {
    return {
        id: item.id,
        title: buildErrorItemTitle(item),
        subjectId: item.subjectId,
        subjectName: item.subject?.name ?? "未分类",
        tags: item.tags.map((tag) => tag.name),
        createdAt: item.createdAt.toISOString(),
        updatedAt: item.updatedAt.toISOString(),
        mastered: item.masteryLevel > 0,
    };
}

export function mapMobileErrorItemDetail(item: ErrorItemWithRelations) {
    return {
        id: item.id,
        title: buildErrorItemTitle(item),
        subjectName: item.subject?.name ?? "未分类",
        questionText: item.questionText,
        answer: item.answerText,
        analysis: item.analysis,
        wrongAnswerText: item.wrongAnswerText,
        mistakeAnalysis: item.mistakeAnalysis,
        mistakeStatus: item.mistakeStatus,
        tags: item.tags.map((tag) => tag.name),
        imageUrl: item.originalImageUrl,
        masteryLevel: item.masteryLevel,
        gradeSemester: item.gradeSemester,
        paperLevel: item.paperLevel,
        geogebraCommands: item.geogebraCommands,
        createdAt: item.createdAt.toISOString(),
        updatedAt: item.updatedAt.toISOString(),
    };
}

export function mapMobilePracticeQuestion(question: {
    questionText?: string;
    answerText?: string;
    analysis?: string;
    subject?: string;
    knowledgePoints?: string[];
    requiresImage?: boolean;
}) {
    const questionText = question.questionText ?? "";
    return {
        title: buildAnalyzeTitle(questionText),
        questionText,
        answer: question.answerText ?? "",
        analysis: question.analysis ?? "",
        subjectName: question.subject ?? "其他",
        tags: Array.isArray(question.knowledgePoints) ? question.knowledgePoints : [],
        requiresImage: question.requiresImage === true,
    };
}

export function buildAnalyzeTitle(questionText: string) {
    const normalized = questionText.replace(/\s+/g, " ").trim();
    if (!normalized) {
        return "未命名错题";
    }
    return normalized.length > 48 ? `${normalized.slice(0, 48)}...` : normalized;
}

function buildErrorItemTitle(item: ErrorItem) {
    const source =
        item.questionText?.trim() ||
        item.ocrText?.trim() ||
        item.source?.trim() ||
        "未命名错题";

    const normalized = source.replace(/\s+/g, " ");
    return normalized.length > 48 ? `${normalized.slice(0, 48)}...` : normalized;
}

function readBearerToken(req: Request) {
    const authorization = req.headers.get("authorization");
    if (!authorization?.startsWith("Bearer ")) {
        return null;
    }
    return authorization.slice("Bearer ".length).trim();
}

function signAccessToken(userId: string) {
    const now = Math.floor(Date.now() / 1000);
    const payload: MobileAccessTokenPayload = {
        sub: userId,
        type: "access",
        iat: now,
        exp: now + ACCESS_TOKEN_TTL_SECONDS,
    };
    const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
    const signature = sign(body);
    return `${body}.${signature}`;
}

function verifyAccessToken(token: string): MobileAccessTokenPayload | null {
    const [body, signature] = token.split(".");
    if (!body || !signature || !safeEqual(signature, sign(body))) {
        return null;
    }

    try {
        const payload = JSON.parse(
            Buffer.from(body, "base64url").toString("utf8")
        ) as MobileAccessTokenPayload;

        if (payload.type !== "access" || payload.exp < Math.floor(Date.now() / 1000)) {
            return null;
        }

        return payload;
    } catch {
        return null;
    }
}

function sign(body: string) {
    return createHmac("sha256", getMobileAuthSecret())
        .update(body)
        .digest("base64url");
}

function safeEqual(left: string, right: string) {
    const leftBuffer = Buffer.from(left);
    const rightBuffer = Buffer.from(right);

    if (leftBuffer.length !== rightBuffer.length) {
        return false;
    }

    return timingSafeEqual(leftBuffer, rightBuffer);
}

function getMobileAuthSecret() {
    const secret =
        process.env.MOBILE_AUTH_SECRET ||
        process.env.NEXTAUTH_SECRET ||
        process.env.AUTH_SECRET;

    if (secret) {
        return secret;
    }

    if (process.env.NODE_ENV === "production") {
        throw new Error("MOBILE_AUTH_SECRET or NEXTAUTH_SECRET is required");
    }

    return "dev-mobile-auth-secret";
}
