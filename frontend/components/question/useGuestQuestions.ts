"use client"

import { getGuestSession } from "@/lib/api/guestSession";
import { QuestionEvent, QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";

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
        guestLoad();
    }, [eventId, sort]);

    useEffect(() => {
        const client = new Client({
            brokerURL: "ws://localhost:8081/ws",
            reconnectDelay: 5000,

            onConnect: () => {
                client.subscribe(
                    `/topic/events/${eventId}/questions`,
                    (message) => {
                        const questionEvent: QuestionEvent =
                            JSON.parse(message.body);

                        setQuestions(previousQuestions => {
                            const questionExists = previousQuestions.some(
                                question =>
                                    question.id === questionEvent.question.id
                            );

                            const updatedQuestions: QuestionResponse[] = questionExists
                                ? previousQuestions.map(question =>
                                    question.id === questionEvent.question.id
                                        ? { ...question, ...questionEvent.question }
                                        : question
                                )
                                : [
                                    {
                                        ...questionEvent.question,
                                        votedByMe: false
                                    },
                                    ...previousQuestions
                                ];

                            return [...updatedQuestions].sort((first, second) => {
                                if (sort === "popular") {
                                    return second.upvoteCount - first.upvoteCount;
                                }

                                return Date.parse(second.createdAt)
                                    - Date.parse(first.createdAt);
                            });
                        });
                    }
                );

            },
        });

        client.activate();

        return () => {
            client.deactivate();
        };
    }, [eventId, sort]);

    return { questions, loading, error, guestLoad, applyVote};
}
