"use client"

import { fetchPostPollResponse } from "@/lib/api/pollApi";
import { useState } from "react";

export default function useAnswerPoll(pollId: number){

    const [ pending, setPending ] = useState(false);
    const [ error, setError ] = useState("");
    
    async function handleSubmit(optionIds: number[])
    : Promise<boolean>
    {

        try{
            setPending(true);
            setError("");
            await fetchPostPollResponse(pollId, optionIds)

            return true;
        }catch(e){
            setError(e instanceof Error ? e.message : "エラーが発生しました")
            return false;
        }finally{
            setPending(false);
        }
    }

    return{
        handleSubmit,
        pending,
        error,
    }
}