"use client"

import { getGuestSession } from "@/lib/api/client/guestSession";
import { fetchGetQuestion } from "@/lib/api/client/questionApi";
import { QuestionFilter, QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useRef, useState } from "react";
import { subscribeQuestions } from "@/lib/stomp/questionStomp";
import { receiveEvent } from "@/lib/domain/question/questionEvent";

export default function useQuestions(eventId: number, sort: Sort, filter: QuestionFilter, isHost: boolean) {

    const [questions, setQuestions] = useState<QuestionResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    function applyVote(questionId: number, upvoteCount: number, votedByMe: boolean) {
        setQuestions(prev =>
            prev.map(q => (q.id === questionId ? { ...q, upvoteCount, votedByMe } : q))
        );
    }

    async function load() {

        setLoading(true);
        setError(null);

        try {
            if(!isHost){
                await getGuestSession();
            }
            
            const data = await fetchGetQuestion(eventId, sort, filter);
            setQuestions(data);
        } catch (e) {
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        load();
    }, [eventId, sort, filter]);

    const viewRef = useRef({sort, filter});
    viewRef.current = { sort, filter };

    useEffect(() => {
            return subscribeQuestions(eventId, (questionEvent) => {
                const { sort, filter } = viewRef.current;

            setQuestions(previousQuestions => 
                receiveEvent(previousQuestions, questionEvent, filter, sort, isHost)
                );
            });
    },[eventId]);

    return { questions, loading, error, load, applyVote };
}
