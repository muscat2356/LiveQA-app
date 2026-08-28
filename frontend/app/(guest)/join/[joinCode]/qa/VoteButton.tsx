"use client"

import { readCookie } from "@/lib/http/cookie";
import { Button } from "@/components/ui/button";
import { VoteResponse } from "@/types/question/question";
import { RiHeart3Fill, RiHeart3Line } from "@remixicon/react";
import { useState } from "react";
import { toast } from "sonner";

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

            const xsrfToken = readCookie("XSRF-TOKEN");
            const res = await fetch(`/api/questions/${questionId}/vote`, {
                    method: votedByMe ? "DELETE" : "POST",
                    headers:  { 
                        ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
                    },
                    credentials: "same-origin",
            });

            if(!res.ok){
                const problem = await res.json().catch(() => null);

                if(res.status === 401){
                toast.error("セッションが切れました。ページを読み込みください")
                }

                if(res.status === 409){
                    toast.error(problem?.message ?? "既に投票済みです")
                    onSync?.();
                    return;
                }
                
            }

            

            const voted : VoteResponse = await res.json();
            applyVote(voted.questionId, voted.upvoteCount, voted.votedByMe);
            toast.success(votedByMe ? "いいねを取り消しました" : "いいねを押しました" );

        }catch (e){
            toast.error(e instanceof Error ? e.message : "エラーが発生ました");
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