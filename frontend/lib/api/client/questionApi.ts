import { apiClient } from "@/lib/http/apiClient";
import { QuestionFilter, QuestionResponse, Sort } from "@/types/question/question";

/**
 * 質問の取得 クライアント用
 * @param eventId 
 * @param sort 並び替え用　popular newestの二択
 * @param filter フィルター用 回答済み　未回答　非公開
 * @returns 並び替えなられた質問を一覧でリターン
 */
export async function fetchGetQuestion(eventId: number, sort:Sort, filter: QuestionFilter){

        const params = new URLSearchParams({ sort });
        if(filter !== "ALL"){
            params.set("status", filter);
        }

        return apiClient<QuestionResponse[]>(
            `/api/events/${eventId}/questions?${params}`
        );
}