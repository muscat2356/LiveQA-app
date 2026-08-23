"use client"

import { readCookie } from "@/components/cookie";
import { Button } from "@/components/ui/button";
import { nextQuestionStatusMap, questionActionLabelMap, QuestionStatus } from "@/types/question/question";
import { useState } from "react";

export default function QuestionStatusButton({
    questionId, 
    status, 
    load,
}:{
    questionId: number;
    status: QuestionStatus;
    load: () => void;
}){
    const [ pending, setPending ] = useState(false);
    const [ errorMessage, setErrorMessage] = useState<string | null>(null);

    const nextStatus = nextQuestionStatusMap[status];

    if (nextStatus.length === 0) return null;

    async function handleUpdate(target: QuestionStatus){

        setPending(true);
        setErrorMessage(null);
        try{
            const xsrfToken = readCookie("XSRF-TOKEN");
            const res = await fetch(`/api/questions/${questionId}/status`, {
                method: "PATCH",
                headers:  { 
                    "Content-Type": "application/json",
                    ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
                },
                credentials: "same-origin",
                body: JSON.stringify({ status: target }),
            });

            if(!res.ok){
                const problem = await res.json().catch(() => null)
                throw new Error(problem?.message ?? "ステータス更新のエラーが発生しました")
            }

            load();

        }catch (e){
            console.error(e);
            setErrorMessage(e instanceof Error ? e.message : "ステータス更新のエラーが発生しました");
        }finally{
            setPending(false);
        }
    };


    return(
        <div className="mt-3 space-y-2 border-t pt-3">
            {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}
            <div className="flex flex-wrap gap-2">
                {nextStatus.map((target) => (
                    <Button
                        key={target}
                        size="sm"
                        variant={target === "ARCHIVED" ? "outline" : "default"}
                        onClick={() => handleUpdate(target)}
                        disabled={pending}
                    >
                        {pending ? "更新中..." : questionActionLabelMap[target]}
                    </Button>
                ))}
            </div>
        </div>
    );
}