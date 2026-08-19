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
}: {
    questionId: number;
    upvoteCount: number;
    votedByMe: boolean;
    applyVote: (questionId: number, upvoteCount: number, votedByMe: boolean) => void;
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
                throw new Error(problem?.message??"再度実行してください")
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
            <Button onClick={handleVote} disabled={pending}>
                👍{upvoteCount}
            </Button>
        </div>
    );
}