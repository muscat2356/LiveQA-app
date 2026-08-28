import { readCookie } from "@/lib/http/cookie";
import { JoinEvent } from "@/types/event/event";

export async function fetchJoinEvent(joinCode: string): Promise<JoinEvent | null>{

    const res = await fetch(
        `${process.env.API_BASE_URL}/api/join/${encodeURIComponent(joinCode)}`,
        { cache: "no-store", headers: { Accept: "application/json" } }
    );

    if(res.status === 404){
        return null;
    }

    if(!res.ok){
        throw new Error(`join API failed: ${res.status}`);
    }
    
    return res.json();
}

export async function updateEvent( title: string, eventId: number){

    const xsrfToken = readCookie("XSRF-TOKEN");

    const res = await fetch(`/api/events/${eventId}`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
            ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
        },
        credentials: "same-origin",
        body: JSON.stringify({
            title,
        }),
    });

    if(res.status === 404){
        throw new Error("イベントが存在しないため、更新ができません")
    }

    if(!res.ok){
        throw new Error("イベント名更新に失敗しました");
    }

    return res.json();

}

export async function deleteEvent(eventId: number){
    
    const xsrfToken = readCookie("XSRF-TOKEN");

    const res = await fetch(`/api/events/${eventId}`,{
        method: "DELETE",
        headers: {
            ...(xsrfToken 
                ? {"X-XSRF-TOKEN" : xsrfToken} 
                : {}),
        },
        credentials: "same-origin",
    });

    if(res.status === 404){
        throw new Error("イベントが存在していません")
    }

    if(!res.ok){
        throw new Error("イベント削除に失敗しました")
    }
}