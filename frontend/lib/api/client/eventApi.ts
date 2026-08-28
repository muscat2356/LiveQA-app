import { apiClient } from "@/lib/http/apiClient";
import { EventDetailResponse } from "@/types/event/event";

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