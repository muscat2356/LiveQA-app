import { readCookie } from "@/components/cookie";
import { PollDto, PollStatus, PollType } from "@/types/poll/poll";

export async function fetchPostPoll(id: number, questionText: string, type: PollType, options: string[]){

    const xsrfToken = readCookie("XSRF-TOKEN");

    const res = await fetch(
        `${process.env.API_BASE_URL}/api/events/${id}/polls`,{ 
            method: "POST",
            headers: { 
                "Content-Type":"application/json",
                ...(xsrfToken ? { "X=XSRF-TOKEN" : xsrfToken} : {}),
            },
            credentials: "same-origin",
            body: JSON.stringify({
                questionText, type, options
            })
            }
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

export async function fetchGetPoll(id: number){

    const res = await fetch(
        `${process.env.API_BASE_URL}/api/events/${id}/polls`,
        { cache: "no-store", headers: { Accept: "application/json" } }
    );

    if(res.status === 404){
        throw new Error("イベントが存在しません");
    }

    if(!res.ok){
        throw new Error("エラーが発生しました");
    }

    return res.json();

}

export async function fetchPatchPoll(pollId: number, status: PollStatus){

    const xsrfToken = readCookie("X-XSRF-TOKEN");

    const res = await fetch(
        `${process.env.API_BASE_URL}/api/polls/${pollId}/polls`,{ 
            method: "PATCH",
            headers: { 
                "Content-Type":"application/json",
                ...(xsrfToken ? { "X=XSRF-TOKEN" : xsrfToken} : {}),
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

    return res.json();
}

