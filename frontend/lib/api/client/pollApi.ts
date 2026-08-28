import { readCookie } from "@/lib/http/cookie";
import { PollCreateRequest, PollDto} from "@/types/poll/poll";
import { apiClient } from "../../http/apiClient";

/**
 * ポール設問＋選択肢をまとめて作成API
 * 作成直後はステータス：DRAFT
 * @param id 
 * @param pollCreateRequest 
 * @returns 
 */
export async function fetchPostPoll(
    id: number, 
    pollCreateRequest:PollCreateRequest
):
    Promise<PollDto>
{

    return apiClient<PollDto>(`/api/events/${id}/polls`,{
        method: "POST",
        body: pollCreateRequest ,
    });
}

/**
 * ポール取得API
 * @param id 
 * @returns 
 */
export async function fetchGetPoll(id: number):Promise<PollDto[]>{

    return apiClient<PollDto[]>(`/api/events/${id}/polls`);
}

/**
 * ポールステータス更新API
 * @param pollId 
 * @param status 
 * @returns 
 */
export async function fetchPatchPollStatus(
    pollId: number, 
    status: "OPEN" | "CLOSED"
):
    Promise<PollDto>
{

    return apiClient<PollDto>(`/api/polls/${pollId}/status`,{
            method: "PATCH",
            body: {status},
    })
}

/**
 * ポール回答集計API
 * 選択肢への回答
 * @param pollId 
 * @param optionIds 
 * @returns 
 */
export async function fetchPostPollResponse(
    pollId: number, 
    optionIds: number[]
):
    Promise<PollDto>
{

    return apiClient(`/api/polls/${pollId}/responses`,{
            method: "POST",
            body: {optionIds},
    })
} 