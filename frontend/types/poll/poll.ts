
export type PollDto = {
    id: number;
    eventId: number;
    questionText: string;
    type: PollType;
    status: PollStatus;
    options: option[];
    totalResponses: number;
    myOptionIds: number[];
}

export type PollType = "SINGLE" | "MULTI";

export type PollStatus = "DRAFT" | "OPEN" | "CLOSED";

export type option = {
    id: number;
    label: string;
    count: number;
}

export type PollCreateRequest = {
    questionText: string;
    type: PollType;
    options: string[];
}

