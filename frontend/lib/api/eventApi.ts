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