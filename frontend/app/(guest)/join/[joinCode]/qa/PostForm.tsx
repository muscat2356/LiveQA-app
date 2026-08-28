"use client"

import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useState } from "react"
import { toast } from "sonner";
import { fetchPostQuestion } from "@/lib/api/client/questionApi";

/**
 * 質問入力フォームUI
 * @param 該当イベントID
 * @param 質問入力者名
 */
export default  function PostForm({
    eventId,
    nickname,
    onPosted,
} : { 
    eventId: number;
    nickname: string;
    onPosted: () => void;
}){
    const [ body , setBody] = useState("");
    const [ pending , setPending ] = useState(false);
    const [ errorMessage, setErrorMessage ] = useState<string | null>(null);

    async function handlePost(){

        const trimmedBody = body.trim();

        if(trimmedBody){
            setErrorMessage("質問を入力してください。")
            return;
        }

        setPending(true);
        setErrorMessage(null);

        try{
            await fetchPostQuestion(eventId, nickname, trimmedBody);
            
            setBody("");
            onPosted();

            toast.success("質問を投稿しました");

        }catch (e){
            setErrorMessage(e instanceof Error ? e.message : "エラーが発生しました");
            
        }finally{
            setPending(false);

        }
    }

    return(
        <div className="sticky bottom-4 space-y-3 rounded-2xl border bg-card p-4 shadow-lg ring-1 ring-foreground/5">
            <Textarea
                value={body}
                onChange={(e) => setBody(e.target.value)}
                placeholder="質問を入力してください。"
                maxLength={500}
                className="min-h-24 resize-none"
            />

            {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}

            <div className="flex items-center justify-between gap-3">
                <p className="text-xs text-muted-foreground">{nickname || "匿名"}として投稿</p>

                <Button onClick={handlePost} disabled={pending}>
                    {pending ? "送信中" : "送信"}
                </Button>
            </div>
        </div>
    );
}