"use client"

import { QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useState } from "react";

export default function useQuestions(eventId: number, sort: Sort){
    
    const [questions, setQuestions] = useState<QuestionResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    function applyVote(questionId: number, upvoteCount: number, votedByMe: boolean){
        setQuestions(prev =>
            prev.map(q => (q.id === questionId ? { ...q, upvoteCount, votedByMe } : q))
        );  
    }

    async function load(){
        
        setLoading(true);
        setError(null);

        try{

            const res = await fetch(`/api/events/${eventId}/questions?sort=${sort}`,{
             credentials: "same-origin", 
             headers: { Accept: "application/json" }
            });

            if(!res.ok){
                const problem = await res.json().catch(() => null);
                throw new Error(problem?.message ?? "エラーが発生しました");
            }

            const data: QuestionResponse[] = await res.json();
            setQuestions(data);
        }catch (e){
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        }finally{
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, [eventId, sort]);

    return { questions, loading, error, load, applyVote};
}