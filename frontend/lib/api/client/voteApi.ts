import { apiClient } from "@/lib/http/apiClient";
import { VoteResponse } from "@/types/question/question";
/**
 * いいねの追加削除API
 * @param questionId 
 * @param votedByMe 
 * @returns 
 */
export function fetchVote(questionId: number, votedByMe: boolean){

    return apiClient<VoteResponse>(`/api/questions/${questionId}/vote`,{
        method: votedByMe ? "DELETE" : "POST",
        errorMessage: {
            401:"セッションが切れました。ページを読み込みください",
            409: "既に投票済みです"
    }});
}

