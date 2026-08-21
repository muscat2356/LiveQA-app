"use client"

import { readCookie } from "@/components/cookie";
import { Button } from "@/components/ui/button";
import { VoteResponse } from "@/types/question/question";
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
        <div>
            <Button
                onClick={handleVote}
                disabled={pending}
                size="sm"
                variant={votedByMe ? "default" : "outline"}
                className="gap-1 tabular-nums"
            >
                {votedByMe ? "♥" : "♡"} {upvoteCount}
            </Button>
        </div>
    );
}