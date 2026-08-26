import { readCookie } from "@/components/cookie";
import { PollCreateRequest, PollDto} from "@/types/poll/poll";

export async function fetchPostPoll(
    id: number, 
    pollCreateRequest:PollCreateRequest
):
    Promise<PollDto>
{

    const xsrfToken = readCookie("XSRF-TOKEN");

    const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/events/${id}/polls`,{ 
            method: "POST",
            headers: { 
                "Content-Type":"application/json",
                ...(xsrfToken ? { "X-XSRF-TOKEN" : xsrfToken} : {}),
            },
            credentials: "same-origin",
            body: JSON.stringify(
                pollCreateRequest
            )}
    );

    if(res.status === 400){
        throw new Error("選択肢は2〜10個にしてください");
    }

    if(!res.ok){
        throw new Error("エラーが発生しました");
    }

    const pollDto: PollDto = await res.json();

    return pollDto;
}

export async function fetchGetPoll(id: number):Promise<PollDto[]>{

    const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/events/${id}/polls`,
        { cache: "no-store", headers: { Accept: "application/json" } }
    );

    if(res.status === 404){
        throw new Error("イベントが存在しません");
    }

    if(!res.ok){
        throw new Error("エラーが発生しました");
    }

    const polls : PollDto[] = await res.json();
    return polls;

}

export async function fetchPatchPollStatus(
    pollId: number, 
    status: "OPEN" | "CLOSED"
):
    Promise<PollDto>
{

    const xsrfToken = readCookie("XSRF-TOKEN");

    const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/polls/${pollId}/status`,{ 
            method: "PATCH",
            headers: { 
                "Content-Type":"application/json",
                ...(xsrfToken ? { "X-XSRF-TOKEN" : xsrfToken} : {}),
            },
            credentials: "same-origin",
            body: JSON.stringify({ status }),
    });


    if(res.status === 400){
        throw new Error("status は OPEN / CLOSED のいずれかです。")
    }

    if(res.status === 409){
        throw new Error("この状態遷移はできません。")
    }

    if(res.status === 404){
        throw new Error("ポールまたはイベントが存在しません")
    }

    if(!res.ok){
        throw new Error("エラーが発生しました")
    }

    const poll : PollDto = await res.json();
    return poll;
}

