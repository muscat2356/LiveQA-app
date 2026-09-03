"use client"

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { updateEvent } from "@/lib/api/client/eventApi";

import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";

export default function UpdateEventNameForm({
    eventId, 
    currentName,
}: {
    eventId: number;
    currentName: string;
}){

    const [ name, setName ] = useState("");
    const [ pending, setPending ] = useState(false);
    const [ error, setError ] = useState("");

    const router = useRouter();

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if(!name.trim()){
            setError("イベント名を入力してください")
            return;
        }

        if(name.trim() === currentName){
            setError("変更するイベント名を入力してください")
            return;
        }

        try{
            setPending(true);
            setError("");

            await updateEvent(name.trim(), eventId);

            router.push(`/dashboard`);

        }catch(e){
            if(e instanceof Error){
                setError(e.message);
            }else{
                setError("エラーが発生しました");
            }
        }finally{
            setPending(false);
        }
    }

    return(
        <form onSubmit={handleSubmit} className="w-full space-y-3">
            <div className="space-y-1.5">
                <Label htmlFor="eventName">イベント名更新</Label>
                <Input
                    id="eventName"
                    type="text"
                    value={name}
                    placeholder="更新するイベント名を入力してください。"
                    onChange={(e) => setName(e.target.value)}
                    disabled={pending}
                    className="h-11"
                />
            </div>

            {error && (
                <p className="text-sm text-destructive">{error}</p>
            )}

            <Button type="submit" disabled={pending} className="w-full sm:w-auto">
                {pending ? "更新中" : "更新"}
            </Button>
        </form>
    )
}