import { orNull } from "@/lib/http/orNull";
import { serverApiClient } from "@/lib/http/serverApiClient";
import { EventDetailResponse, EventListResponse, JoinEvent } from "@/types/event/event";
/**
 * 参加コードからイベントを取得する　Server component ServerActions用
 * @param joinCode 
 * @returns 
 */
export async function fetchJoinEvent(joinCode: string): Promise<JoinEvent | null>{
    return orNull(
            serverApiClient<JoinEvent>
                (`/api/join/${encodeURIComponent(joinCode)}`),
                [404]
            );
}

/**
 * イベント全件取得API
 * @returns 
 */
export async function fetchGetEvent():Promise<EventListResponse[] | null>{
    return orNull(
        serverApiClient<EventListResponse[]>(
            `/api/events`,
        ),
        [401]
    );
}

/**
 * 該当イベント詳細取得API
 * @param id 
 * @returns 
 */
export async function fetchGetEventDetail(id: string):Promise<EventDetailResponse | null> {
    return orNull(
        serverApiClient<EventDetailResponse>(
                `/api/events/${id}`,
        ),
        [404, 403]
    );
}
