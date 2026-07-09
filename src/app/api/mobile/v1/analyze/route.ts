import { getAIService } from "@/lib/ai";
import { calculateGrade } from "@/lib/grade-calculator";
import { calculateGradeNumber, inferSubjectFromName } from "@/lib/knowledge-tags";
import { prisma } from "@/lib/prisma";
import {
    buildAnalyzeTitle,
    getMobileUser,
    mobileError,
    mobileOk,
} from "@/lib/mobile-api";

export const runtime = "nodejs";

const subjectNameMapping: Record<string, string> = {
    math: "数学",
    physics: "物理",
    chemistry: "化学",
    biology: "生物",
    english: "英语",
    chinese: "语文",
    history: "历史",
    geography: "地理",
    politics: "政治",
};

export async function POST(req: Request) {
    const user = await getMobileUser(req);
    if (!user) {
        return mobileError(401, "UNAUTHORIZED", "Authentication required");
    }

    const formData = await req.formData();
    const image = formData.get("image");

    if (!(image instanceof File)) {
        return mobileError(400, "MISSING_IMAGE", "Missing image file");
    }

    const subjectId = formData.get("subjectId")?.toString();
    const language = formData.get("language")?.toString() === "en" ? "en" : "zh";
    const imageBase64 = Buffer.from(await image.arrayBuffer()).toString("base64");
    const mimeType = image.type || "image/jpeg";

    let subjectChinese: string | null = null;
    if (subjectId) {
        const subject = await prisma.subject.findFirst({
            where: {
                id: subjectId,
                userId: user.id,
            },
            select: { name: true },
        });
        const subjectKey = subject ? inferSubjectFromName(subject.name) : null;
        subjectChinese = subjectKey ? subjectNameMapping[subjectKey] ?? null : null;
    }

    const userGrade = calculateGradeNumber(user.educationStage, user.enrollmentYear);
    const userGradeSemester =
        user.educationStage && user.enrollmentYear
            ? calculateGrade(user.educationStage, user.enrollmentYear, new Date(), "zh")
            : null;

    try {
        const analysisResult = await getAIService().analyzeImage(
            imageBase64,
            mimeType,
            language,
            userGrade,
            subjectChinese,
            userGradeSemester
        );

        return mobileOk({
            title: buildAnalyzeTitle(analysisResult.questionText),
            subjectName: analysisResult.subject,
            questionText: analysisResult.questionText,
            answer: analysisResult.answerText,
            analysis: analysisResult.analysis,
            tags: analysisResult.knowledgePoints,
        });
    } catch (error) {
        return mobileError(
            500,
            "AI_ERROR",
            "Failed to analyze image",
            error instanceof Error ? error.message : String(error)
        );
    }
}
