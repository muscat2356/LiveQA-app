"use client"

import { fetchPostPoll } from "@/lib/api/pollApi";
import { PollCreateRequest, PollDto } from "@/types/poll/poll";
import { useState } from "react";

export default function useCreatePoll(eventId: number){
    const[ pending, setPending ] = useState(false);
    const[ error, setError ] = useState("");

    async function handleSubmit(pollCreateRequest: PollCreateRequest): Promise<boolean>{
        try{
            setPending(true);
            setError("");

            await fetchPostPoll(eventId, pollCreateRequest);

            return true;

        }catch(e){
            setError(e instanceof Error ? e.message : "エラーが発生しました")
            return false;
        }finally{
            setPending(false);
        }
    }

    return{
        pending, error, handleSubmit
    }
}