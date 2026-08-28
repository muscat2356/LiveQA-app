"use client"

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { EventCreateRequest, SubmitState } from "@/types/event/event";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { readCookie } from "@/lib/http/cookie";

export default function EventForm(){
    const [title, setTitle] = useState("");
    const [state, setState] = useState<SubmitState>("idle");
    const [error, setError] = useState("");
    const router = useRouter();

    async function handleSubmit(){
        if(!title.trim()){
            setError("タイトルを入力してください");
            return;
        }

        setState("submitting");
        setError("");

        const payload: EventCreateRequest = {
            title: title.trim(),
        };

        try{
            const xsrfToken = readCookie("XSRF-TOKEN");
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/events`, {
                method: "POST",
                headers: { 
                    "Content-Type": "application/json",
                    ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
                 },
                 credentials:"include",
                body: JSON.stringify(payload)
            });

            if(!res.ok){
                const problem = await res.json().catch(() => null)
                throw new Error(problem?.detail ?? `HTTP ${res.status}`)
            }

            router.push(`/dashboard`);
        }catch(e){
            console.error(e);
            setError(e instanceof Error ? e.message: "エラー発生しています")
            setState("idle")
        }
    }

    return(
        <div>
            <div className="space-y-4">
                <div className="space-y-1.5">
                    <Label htmlFor="title">タイトル名</Label>
                    <Input
                        id="title"
                        placeholder="イベントを入力してください"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        className="h-11"></Input>
                </div>

                {error && (
                    <p className="text-sm text-destructive">{error}</p>
                    )}

                <Button
                    onClick={handleSubmit}
                    disabled={state === "submitting" || !title.trim()}
                    size="lg"
                    className="h-11 w-full">
                    {state === "submitting" ? "作成中..." : "新規作成"}
                </Button>

            </div>
        </div>
    );
}
