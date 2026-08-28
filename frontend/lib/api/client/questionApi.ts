import { apiClient } from "@/lib/http/apiClient";
import { QuestionFilter, QuestionResponse, QuestionStatus, Sort } from "@/types/question/question";

/**
 * 質問の取得API
 * @param eventId 
 * @param sort 並び替え用　popular newestの二択
 * @param filter フィルター用 回答済み　未回答　非公開
 * @returns 並び替えなられた質問を一覧でリターン
 */
export async function fetchGetQuestion(
    eventId: number, 
    sort:Sort, 
    filter: QuestionFilter
):
    Promise<QuestionResponse[]>
{

        const params = new URLSearchParams({ sort });
        if(filter !== "ALL"){
            params.set("status", filter);
        }

        return apiClient<QuestionResponse[]>(
            `/api/events/${eventId}/questions?${params}`
        );
}

/**
 * 質問投稿API
 * @param eventId 
 * @param body 
 * @param nickname 
 * @returns 
 */
export async function fetchPostQuestion(
    eventId: number, 
    body: string,
    nickname: string,
):
    Promise<QuestionResponse>
{
    return apiClient(`/api/events/${eventId}/questions`, {
                method: "POST",
                body:{ 
                    body,
                    nickname: nickname || null 
                },
                errorMessage:{
                    400:"再度実行してください"
                },
    });
}

/**
 * 質問ステータス更新API
 * @param questionId 
 * @param target 
 * @returns 
 */
export async function fetchPatchQuestionStatus(
    questionId: number,
    target: QuestionStatus,
):
    Promise<void>
{
    return apiClient(`/api/questions/${questionId}/status`,{
        method: "PATCH",
        body: { target },
    });
}
