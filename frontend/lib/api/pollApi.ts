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
        `/api/events/${id}/polls`,{ 
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

    if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "エラーが発生しました");
    }

    //移し替え
    const pollDto: PollDto = await res.json();

    return pollDto;
}

export async function fetchGetPoll(id: number):Promise<PollDto[]>{

    const res = await fetch(
        `/api/events/${id}/polls`,{  
            headers: { Accept: "application/json",} ,
            credentials: "same-origin",
            cache: "no-store"
        }
    );

    if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "エラーが発生しました");
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
        `/api/polls/${pollId}/status`,{ 
            method: "PATCH",
            headers: { 
                "Content-Type":"application/json",
                ...(xsrfToken ? { "X-XSRF-TOKEN" : xsrfToken} : {}),
            },
            credentials: "same-origin",
            body: JSON.stringify({ status }),
    });


    if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "エラーが発生しました");
}

    const poll : PollDto = await res.json();
    return poll;
}

