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



