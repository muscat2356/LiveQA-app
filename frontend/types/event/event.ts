//イベントステータス設定
export type EventStatus = "DRAFT" | "OPEN" | "CLOSED";

//GET /api/events （HOST) 自分のイベント一覧。
//Response 200
export type EventListResponse = {
    id: number;
    title: string;
    joinCode: string;
    status: EventStatus;
    questionCount: number;
    createdAt: string;
}

//POST /api/events （HOST）イベント作成
//Request 
export type EventCreateRequest = {
    title: string;
}

//POST /api/events （HOST）イベント作成
//Response 201
export type EventCreateResponse = {
    id: number;
    title: string;
    joinCode: string;
    joinUrl: string;
    status: EventStatus;
    createdAt: string;
}






