"use client"

import { PollDto } from "@/types/poll/poll";

export function OrganizerChart({
    pending,
    error,
    poll
}:{
    pending: boolean;
    error: string;
    poll: PollDto | null;
}) {

    if (pending && !poll) return <p className="py-6 text-center text-sm text-muted-foreground">読み込み中・・・</p>;
    if (error && !poll)   return <p className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</p>;
    if (!poll)   return null;


    return(
        <div>
            
        </div>
    );
}