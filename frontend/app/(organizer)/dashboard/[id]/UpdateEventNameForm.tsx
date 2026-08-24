"use client"

import { Button } from "@/components/ui/button";
import { updateEvent } from "@/lib/api/eventApi";
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function UpdateEventNameForm({
    eventId, 
    cuurentName,
}: {
    eventId: number;
    cuurentName: string;
}){

    const [ name, setName ] = useState("");
    const [ pending, setPending ] = useState(false);
    const [ error, setError ] = useState("");

    const router = useRouter();

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if(!name.trim()){
            setError("イベント名を入力してください")
            return;
        }

        if(name === cuurentName){
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
        <form onSubmit={handleSubmit}>
            <div>
                <label htmlFor="eventName">イベント名更新</label>
                <input 
                    id="eventName"
                    type="text" 
                    value={name}
                    placeholder="更新するイベント名を入力してください。"
                    onChange={(e) => setName(e.target.value)}
                    disabled={pending}
                />
            </div>

            {error && (
                <p>{error}</p>
            )}

            <Button type="submit" disabled={pending}>
                {pending ? "更新中" : "更新"}
            </Button>
        </form>
    )
}