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
    const actionStatus = questionActionLabelMap[status];

    if (!nextStatus || !actionStatus) return null;

    async function handleUpdate(){

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
                body: JSON.stringify({ status: nextStatus }),
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
        <div>
            {errorMessage && <p>{errorMessage}</p>}
            <Button onClick={handleUpdate} disabled={pending}>
                {pending ? "更新中..." : actionStatus}
            </Button>
        </div>
    );
}