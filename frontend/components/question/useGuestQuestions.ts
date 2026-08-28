"use client"

import { getGuestSession } from "@/lib/api/guestSession";
import { fetchGetQuestion } from "@/lib/api/questionApi";
import { getWebSocketBrokerUrl } from "@/lib/websocket";
import { QuestionEvent, QuestionFilter, QuestionResponse, Sort } from "@/types/question/question";
import { useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";

export default function useGuestQuestions(eventId: number, sort: Sort, filter: QuestionFilter) {

    const [questions, setQuestions] = useState<QuestionResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    function applyVote(questionId: number, upvoteCount: number, votedByMe: boolean) {
        setQuestions(prev =>
            prev.map(q => (q.id === questionId ? { ...q, upvoteCount, votedByMe } : q))
        );
    }

    async function guestLoad() {

        setLoading(true);
        setError(null);

        try {
            await getGuestSession();

            const data = await fetchGetQuestion(eventId, sort, filter);
            setQuestions(data);
        } catch (e) {
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        guestLoad();
    }, [eventId, sort, filter]);

    useEffect(() => {
        const client = new Client({
            brokerURL: getWebSocketBrokerUrl(),
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

                            const matchesFilter =
                                questionEvent.question.status !== "ARCHIVED"
                                && (
                                    filter === "ALL"
                                    || questionEvent.question.status === filter
                                );

                            const updatedQuestions: QuestionResponse[] = questionExists && !matchesFilter
                                ? previousQuestions.filter(question =>
                                    question.id !== questionEvent.question.id
                                )
                                : questionExists
                                    ? previousQuestions.map(question =>
                                        question.id === questionEvent.question.id
                                            ? { ...question, ...questionEvent.question }
                                            : question
                                    )
                                    : matchesFilter
                                        && questionEvent.question.status !== "ARCHIVED" ? [
                                        {
                                            ...questionEvent.question,
                                            votedByMe: false
                                        },
                                        ...previousQuestions
                                    ] : previousQuestions;

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
    }, [eventId, sort, filter]);

    return { questions, loading, error, guestLoad, applyVote };
}
