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
    <div>

        <p>{poll.questionText}</p>
        <p>{poll.type === "SINGLE" ? "単一選択" : "複数選択"}/回答数{poll.totalResponses}件</p>

        <PollStatusButton
            pollId={poll.id}
            status={poll.status}
            load={onChanged}
        />

        <PollResultChart
            poll={poll}
        />
      
    </div>
    );
}