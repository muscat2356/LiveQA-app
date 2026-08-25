import { readCookie } from "@/components/cookie";
import { PollDto, PollType } from "@/types/poll/poll";

export default async function fetchPostPoll(id: number, questionText: string, type: PollType, options: string[]){

    const xsrfToken = readCookie("XSRF-TOKEN")

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
        throw new Error("選択肢は2〜10個にしてください")
    }

    if(!res.ok){
        throw new Error("エラーが発生しました")
    }

    const pollDto: PollDto = await res.json();

    return pollDto;
}

