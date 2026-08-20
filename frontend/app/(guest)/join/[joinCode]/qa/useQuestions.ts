"use client"

import { getGuestSession } from "@/lib/api/guestSession";
import { QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useState } from "react";

export default function useQuestions(eventId: number, sort: Sort){
    
    const [questions, setQuestions] = useState<QuestionResponse[]>([]);
    const [loading, setLoading] = useState(true);

    function applyVote(questionId: number, upvoteCount: number, votedByMe: boolean){
        setQuestions(prev =>
            prev.map(q => (q.id === questionId ? { ...q, upvoteCount, votedByMe } : q))
        );  
    }

    async function load(){
        
        setLoading(true);

        try{
            await getGuestSession();
            const res = await fetch(`/api/events/${eventId}/questions?sort=${sort}`,{
             credentials: "same-origin", 
             headers: { Accept: "application/json" }
            });

            if(!res.ok){

                const problem = await res.json().catch(() => null);

                if(res.status === 403){
                    throw new Error("アクセスが拒否されました")
                }
                throw new Error(problem?.message ?? "再度実行してください。")
            }

            const data: QuestionResponse[] = await res.json();
            setQuestions(data);

        }finally{
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, [eventId, sort]);

    return { questions, loading, load, applyVote};
}