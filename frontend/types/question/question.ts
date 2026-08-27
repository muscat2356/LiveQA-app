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

export type QuestionEventType =
    | "QUESTION_CREATED"
    | "QUESTION_UPVOTED"
    | "QUESTION_ANSWERED";

export type QuestionEventPayload = Omit<QuestionResponse, "votedByMe">;

export type QuestionEvent = {
    type: QuestionEventType;
    question: QuestionEventPayload;
}

export type VoteResponse = {
    questionId: number;
    upvoteCount: number;
    votedByMe: boolean;
}

export type QuestionStatus = "OPEN" | "ANSWERED" | "ARCHIVED";

export type Sort = "popular" | "newest";

export type QuestionFilter = "ALL" | QuestionStatus;

export const nextQuestionStatusMap: Record<QuestionStatus, readonly QuestionStatus[]> = {
  OPEN: ["ANSWERED", "ARCHIVED"],
  ANSWERED: ["ARCHIVED"],
  ARCHIVED: [],
};

/** from から to へ遷移できるか */
export const canTransition = (from: QuestionStatus, to: QuestionStatus): boolean =>
  nextQuestionStatusMap[from].includes(to);

export const questionActionLabelMap: Record<QuestionStatus, string> = {
    OPEN: "未回答に戻す",
    ANSWERED: "回答済みにする",
    ARCHIVED: "非表示にする",
};

export const questionStatusLabel: Record<QuestionStatus, string > = {
    OPEN: "未回答",
    ANSWERED: "回答済み",
    ARCHIVED: "非表示",
}

export const questionFilterLabel: Record<QuestionFilter, string> = {
    ALL: "すべて",
    OPEN: "未回答",
    ANSWERED: "回答済み",
    ARCHIVED: "非表示"
}
