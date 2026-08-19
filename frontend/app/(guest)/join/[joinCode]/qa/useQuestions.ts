"use client"

import { QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useState } from "react";

export default function useQuestions(eventId: number, sort: Sort){
    
    const [questions, setQuestions] = useState<QuestionResponse[]>([]);
    const [loading, setLoading] = useState(true);

    async function load(){
        
        setLoading(true);

        try{

            const res = await fetch(`/api/events/${eventId}/questions?sort=${sort}`,{
             credentials: "same-origin", 
             headers: { Accept: "application/json" }
            });

            if(!res.ok){
                throw new Error("再度実行してください。")
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

    return { questions, loading, load};
}