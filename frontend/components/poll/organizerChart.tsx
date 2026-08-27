"use client"

import { PollDto } from "@/types/poll/poll";
import PollStatusButton from "./pollStatusButton";
import { PollResultChart } from "../guest/PollResultChart";


export function OrganizerChart({
    pending,
    error,
    poll,
    onChanged,
}:{
    pending: boolean;
    error: string;
    poll: PollDto | null;
    onChanged:() => void;
}) {

    if (pending && !poll) return <p className="py-6 text-center text-sm text-muted-foreground">読み込み中・・・</p>;
    if (error && !poll)   return <p className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</p>;
    if (!poll)   return null;


    return (
    <div className="rounded-2xl border bg-card p-4 shadow-sm ring-1 ring-foreground/5">

        <div className="mb-3 flex items-start justify-between gap-3">
            <div className="min-w-0 space-y-1.5">
                <span className="inline-flex w-fit items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">投票</span>
                <p className="text-[15px] font-semibold leading-snug">{poll.questionText}</p>
                <p className="text-xs text-muted-foreground">{poll.type === "SINGLE" ? "単一選択" : "複数選択"} / 回答数 {poll.totalResponses} 件</p>
            </div>

            <div className="shrink-0">
                <PollStatusButton
                    pollId={poll.id}
                    status={poll.status}
                    load={onChanged}
                />
            </div>
        </div>

        <PollResultChart
            poll={poll}
        />

    </div>
    );
}