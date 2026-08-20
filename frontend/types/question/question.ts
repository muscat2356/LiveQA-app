export type QuestionResponse = {
    id: number;
    eventId: number;
    body: string;
    displayName: string | null;
    upvoteCount: number;
    status: QuestionStatus;
    votedByMe: boolean;
    createdAt: string;
};

export type VoteResponse = {
    questionId: number;
    upvoteCount: number;
    votedByMe: boolean;
}

export type QuestionStatus = "OPEN" | "ANSWERED" | "ARCHIVED";

export type Sort = "popular" | "newest";

export const nextQuestionStatusMap: Record<QuestionStatus, QuestionStatus | null > = {
    OPEN: "ANSWERED",
    ANSWERED: "ARCHIVED",
    ARCHIVED: null,
};

export const questionActionLabelMap: Record<QuestionStatus, string | null > = {
    OPEN: "回答済みにする",
    ANSWERED: "非表示にする",
    ARCHIVED: null,
};

export const questionStatusLabel: Record<QuestionStatus, string > = {
    OPEN: "未回答",
    ANSWERED: "回答済み",
    ARCHIVED: "非表示",
}




