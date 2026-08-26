"use client"

import { getGuestSession } from "@/lib/api/guestSession";
import { fetchGetQuestion } from "@/lib/api/questionApi";
import { QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useState } from "react";

export default function useGuestQuestions(eventId: number, sort: Sort){
    
    const [questions, setQuestions] = useState<QuestionResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    function applyVote(questionId: number, upvoteCount: number, votedByMe: boolean){
        setQuestions(prev =>
            prev.map(q => (q.id === questionId ? { ...q, upvoteCount, votedByMe } : q))
        );  
    }

    async function guestLoad(){
        
        setLoading(true);
        setError(null);

        try{
            await getGuestSession();

            const data = await fetchGetQuestion(eventId, sort);
            setQuestions(data);
        }catch (e){
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        }finally{
            setLoading(false);
        }
    }

    useEffect(() => {
        guestLoad();
    }, [eventId, sort]);

    return { questions, loading, error, guestLoad, applyVote};
}