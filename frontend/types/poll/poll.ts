import { EventStatus } from "../event/event";

export type PollDto = {
    id: number;
    evemtId: number;
    questionText: string;
    type: PollType;
    status: EventStatus;
    options: option[];
    totalResponses: number;
    myoptionIds: number[];
}

export type PollType = "SINGLE" | "MULTI";

export type option = {
    id: number;
    label: string;
    count: number;
}

