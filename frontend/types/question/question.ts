import { EventStatus } from "../event/event";

export type QuestionDto = {
    id: number;
    eventId: number;
    body: string;
    displayName: string | null;
    upvoteCount: number;
    status: EventStatus;
    votedByMe: boolean;
    createdAt: string;
};

export type VoteResponse = {
    questionId: number;
    upvotedCount: number;
    votedbyMe: boolean;
}

export type QuestionStatus = "OPEN" | "ANSWERED" | "ARCHIVED";

export type Sort = "popular" | "newest";



