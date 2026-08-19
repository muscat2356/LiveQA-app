//イベントステータス設定
export type EventStatus = "DRAFT" | "OPEN" | "CLOSED";

//GET /api/events （HOST) 自分のイベント一覧。
//Response 200
export type EventListResponse = {
    id: number;
    title: string;
    joinCode: string;
    status: EventStatus;
    createdAt: string;
}

export type EventDetailResponse = {
    id: number;
    title: string;
    joinCode: string;
    joinUrl: string;
    status: EventStatus;
    questionCount: number;
    createdAt: string;
    updatedAt: string;
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

//ステータスコードへの紐づけ
export const statusLabel: Record<EventStatus, string> = {
    DRAFT: "下書き",
    OPEN: "公開中",
    CLOSED: "終了",
};

//variant：shadcn の Badge が受け取る見た目の指定。
// default(濃い) / secondary(灰) / outline(枠だけ) / destructive(赤)の4つ
export const statusVariant: Record<EventStatus, "secondary" | "default" | "outline"> = {
    DRAFT: "secondary",
    OPEN: "default",
    CLOSED: "outline",
};

export const nextStatusMap: Record<EventStatus, Exclude<EventStatus, "DRAFT">> = {
        DRAFT: "OPEN",
        OPEN: "CLOSED",
        CLOSED: "OPEN",
    };

// 現在のステータス → ボタンの文言
export const actionLabelMap: Record<EventStatus, string> = {
    DRAFT: "公開する",
    OPEN: "終了する",
    CLOSED: "再開する",
};

export type EventsStatusResponse = {
    id: string;
    status: EventStatus;
}

export type SubmitState = "idle" | "submitting" | "error";

export type JoinEvent = {
  eventId: number;
  title: string;
  status: EventStatus;
};





