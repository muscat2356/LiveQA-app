"use client"

import { fetchGetQuestion } from "@/lib/api/questionApi";
import { QuestionFilter, QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useState } from "react";

export default function useHostQuestions(eventId: number, sort: Sort, filter: QuestionFilter){
    
    const [questions, setQuestions] = useState<QuestionResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    function applyVote(questionId: number, upvoteCount: number, votedByMe: boolean){
        setQuestions(prev =>
            prev.map(q => (q.id === questionId ? { ...q, upvoteCount, votedByMe } : q))
        );  
    }

    async function hostLoad(){
        
        setLoading(true);
        setError(null);

        try{
            const data: QuestionResponse[] = await fetchGetQuestion(eventId, sort, filter);
            setQuestions(data);
        }catch (e){
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        }finally{
            setLoading(false);
        }
    }

    useEffect(() => {
        hostLoad();
    }, [eventId, sort, filter]);

    return { questions, loading, error, hostLoad, applyVote};
}