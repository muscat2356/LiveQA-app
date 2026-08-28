"use client"

import { Button } from "@/components/ui/button";
import { VoteResponse } from "@/types/question/question";
import { RiHeart3Fill, RiHeart3Line } from "@remixicon/react";
import { useState } from "react";
import { toast } from "sonner";
import { fetchVote } from "@/lib/api/client/voteApi";
import { ApiError } from "@/lib/http/apiError";

/**
 * いいねボタンUI
 * fetchVote->votebymeのPOST/DELETE
 * @param 
 * @returns 
 */
export default function VoteButton({
    questionId,
    upvoteCount,
    votedByMe,
    applyVote,
    onSync,
}: {
    questionId: number;
    upvoteCount: number;
    votedByMe: boolean;
    applyVote: (questionId: number, upvoteCount: number, votedByMe: boolean) => void;
    onSync: () => void;
}){
    const [ pending, setPending] = useState(false);

    async function handleVote(){
        
        setPending(true);

        try{

            const voted : VoteResponse = await fetchVote(questionId, votedByMe);

            applyVote(voted.questionId, voted.upvoteCount, voted.votedByMe);
            toast.success(
                voted.votedByMe ? "いいねを押しました" : "いいねを取り消しました" );

        }catch (e){
            if(e instanceof ApiError && e.status === 409){
                onSync();
            }

            toast.error(e instanceof Error ? e.message : "エラーが発生しました");

        }finally{
            setPending(false);
        }
    }
    return(
        <Button
            onClick={handleVote}
            disabled={pending}
            variant={votedByMe ? "default" : "outline"}
            aria-pressed={votedByMe}
            aria-label={votedByMe ? "いいねを取り消す" : "いいね"}
            className="flex h-auto w-16 flex-col items-center justify-center gap-0.5 rounded-xl px-0 py-2.5 tabular-nums"
        >
            {votedByMe ? <RiHeart3Fill className="size-5" /> : <RiHeart3Line className="size-5" />}
            <span className="text-lg font-bold leading-none">{upvoteCount}</span>
            <span className="text-[10px] font-normal">いいね</span>
        </Button>
    );
}