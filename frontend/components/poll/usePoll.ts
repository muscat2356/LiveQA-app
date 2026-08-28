"use client"

import { getGuestSession } from "@/lib/api/guestSession";
import { fetchGetPoll } from "@/lib/api/pollApi";
import { getWebSocketBrokerUrl } from "@/lib/websocket";
import { PollDto, PollEvent } from "@/types/poll/poll";
import { Client } from "@stomp/stompjs";
import { useCallback, useEffect, useState } from "react";

export default function usePoll(id: number) {

    const [poll, setPoll] = useState<PollDto[]>([]);
    const [pending, setPending] = useState(true);
    const [error, setError] = useState("");

    const load = useCallback(async () => {
        try {

            await getGuestSession();
            
            setPending(true);
            setError("");
            setPoll(await fetchGetPoll(id));
        } catch (e) {
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        } finally {
            setPending(false);
        }
    }, [id]);

    useEffect(() => { load(); }, [load]);

    useEffect(() => {
        const client = new Client({
            brokerURL: getWebSocketBrokerUrl(),
            reconnectDelay: 5000,

            onConnect: () => {
                client.subscribe(
                    `/topic/events/${id}/polls`,
                    (message) => {
                        const pollEvent: PollEvent = JSON.parse(message.body);

                        setPoll(previousPolls =>
                            previousPolls.map(previousPoll =>
                                previousPoll.id === pollEvent.poll.id
                                    ? { ...previousPoll, ...pollEvent.poll }
                                    : previousPoll
                            )
                        );
                    }
                );
            },
        });

        client.activate();

        return () => {
            client.deactivate();
        };
    }, [id]);

    return { poll, pending, error, load };
}
