"use client"

import { readCookie } from "@/components/cookie";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useState } from "react"
import { toast } from "sonner";

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

        if(!body.trim()){
            setErrorMessage("質問を入力してください。")
            return;
        }

        setPending(true);
        setErrorMessage(null);

        try{
            const xsrfToken = readCookie("XSRF-TOKEN");
            const res = await fetch(`/api/events/${eventId}/questions`, {
                method: "POST",
                headers:  { 
                    "Content-Type": "application/json",
                    ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
                },
                credentials: "same-origin",
                body: JSON.stringify({ body: body.trim(), nickname: nickname || null }),
            });
        
            if (!res.ok) {
                const problem = await res.json().catch(() => null);
                throw new Error(problem?.message ?? "再度実行してください");
            }
            
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
        <div>
            <Textarea 
                value={body}
                onChange={(e) => setBody(e.target.value)}
                placeholder="質問を入力してください。"
                maxLength={500}
            />

            {errorMessage && <p>{errorMessage}</p>}

            <p>{nickname || "匿名"}として投稿</p>

            <Button onClick={handlePost} disabled={pending}>
                {pending ? "送信中" : "送信"}
            </Button>
        </div>
    );
}