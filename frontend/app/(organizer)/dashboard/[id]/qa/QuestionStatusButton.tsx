"use client"

import { readCookie } from "@/lib/http/cookie";
import { Button } from "@/components/ui/button";
import { nextQuestionStatusMap, questionActionLabelMap, QuestionStatus } from "@/types/question/question";
import { useState } from "react";
import { fetchPatchQuestionStatus } from "@/lib/api/client/questionApi";
/**
 * 質問ステータス更新ボタンUI
 * @param param0 
 * @returns 
 */
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
            await fetchPatchQuestionStatus(questionId, target)
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