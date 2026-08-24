"use client"

import { Button } from "@/components/ui/button";
import { deleteEvent } from "@/lib/api/eventApi"
import { useRouter } from "next/navigation";
import { useState } from "react";

export default function DeleteButton({ eventId }: { eventId: number }){

    const [ pending, setPending ] = useState(false);
    const [ errorMessage, setErrorMessage ] = useState("");
    const router = useRouter();

    const handleDelete = async () => {
        try{
            setPending(true);
            setErrorMessage("");
            
            await deleteEvent(eventId);

            router.push("/dashboard");
        }catch(e){
            if(e instanceof Error){
                setErrorMessage(e.message);
            }else{
                setErrorMessage("エラーが発生しました")
            }
        }finally{
            setPending(false);
        }
        
    };

    return(
        <div>
            <Button onClick={handleDelete} disabled={pending}>
                {pending ? "削除中" : "削除"}
            </Button>
            {errorMessage &&
                <p>{errorMessage}</p>
            }
        </div>
    );
}