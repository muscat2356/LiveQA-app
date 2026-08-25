"use client"

import { fetchGetPoll } from "@/lib/api/pollApi";
import { PollDto } from "@/types/poll/poll";
import { useCallback, useEffect, useState } from "react";

export default function usePoll(id: number) {

    const [poll, setPoll] = useState<PollDto | null>(null);
    const [pending, setPending] = useState(true);
    const [error, setError] = useState("");

    const load = useCallback(async () => {
        try {
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

    return { poll, pending, error, load };
}