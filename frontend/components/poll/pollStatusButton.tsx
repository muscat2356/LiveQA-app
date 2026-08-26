import { fetchPatchPollStatus } from "@/lib/api/pollApi";
import { PollStatus } from "@/types/poll/poll";
import { useState } from "react";
import { Button } from "../ui/button";

export default function PollStatusButton({
    pollId,
    status,
    load,
}:{
    pollId: number;
    status: PollStatus
    load: () => void;
}){
    
    const [ pending, setPending ] = useState(false);
    const [ error, setError ] = useState("");

    async function change(next: "OPEN" | "CLOSED") {
        try{

            setPending(true);
            setError("");

            await fetchPatchPollStatus(pollId, next);

            load();

        }catch(e){
            setError(e instanceof Error ? e.message: "エラーが発生しました");
        }finally{

            setPending(false);
        }
    }

    if(status === "CLOSED"){
        return <span className="text-sm text-muted-foreground">締切済み</span>;
    }

    return(
        <div>
            {status === "DRAFT" &&(
                <Button onClick={() => change("OPEN")} disabled={pending}>
                    公開
                </Button>
            )}

            {status === "OPEN" &&(
                <Button onClick={() => change("CLOSED")} disabled={pending}>
                    締め切る
                </Button>
            )}

            {error && <span>{error}</span>}
        </div>
    )
}