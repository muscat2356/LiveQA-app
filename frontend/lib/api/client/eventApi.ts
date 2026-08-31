import { apiClient } from "@/lib/http/apiClient";
import { EventCreateRequest, EventDetailResponse } from "@/types/event/event";

/**
 * イベント/クライアント用API
 * @author furuyama
 */

/**
 * イベント名更新API　
 * @param title 変更するタイトル名
 * @param eventId 該当のイベントId
 * @returns
 */
export async function updateEvent( title: string, eventId: number): Promise<EventDetailResponse>{

    return apiClient<EventDetailResponse>(`/api/events/${eventId}`,{
        method: "PATCH",
        body: { title },
        errorMessage: {
            404: "イベントが存在しないため、更新できません"
        },
    })
}

/**
 * イベント削除API
 * @param eventId 
 * @returns 
 */
export async function deleteEvent(eventId: number): Promise<void>{

    return apiClient<void>(`/api/events/${eventId}`, {
        method:"DELETE",
        errorMessage: {
            404: "イベントが存在していないため、削除できません"
        }
    });
}

/**
 * イベント作成API
 * @param payload 
 * @returns 
 */
export async function postEvent(payload: EventCreateRequest):Promise<void>{

    return apiClient<void>(`/api/events`, {
                method: "POST",
                body: payload
    });
}

/**
 * イベントステータス更新API
 * @param eventId 
 * @param nextStatus リクエストに活用（次のステータス）
 * @returns 
 */
export async function patchEventStatus(eventId: number, nextStatus: string):Promise<void>{

    return apiClient<void>(`/api/events/${eventId}/status`,{
        method: "PATCH",
        body: {nextStatus}
    });
}

