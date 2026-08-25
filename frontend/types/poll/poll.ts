
export type PollDto = {
    id: number;
    evemtId: number;
    questionText: string;
    type: PollType;
    status: PollStatus;
    options: option[];
    totalResponses: number;
    myoptionIds: number[];
}

export type PollType = "SINGLE" | "MULTI";

export type PollStatus = "DRAFT" | "OPEN" | "CLOSED";

export type option = {
    id: number;
    label: string;
    count: number;
}

