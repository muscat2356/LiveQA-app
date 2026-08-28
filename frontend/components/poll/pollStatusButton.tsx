import { fetchPatchPollStatus } from "@/lib/api/client/pollApi";
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

        }catch(e){
            setError(e instanceof Error ? e.message: "エラーが発生しました");
        }finally{
            setPending(false);
            load();
        }
    }

    if(status === "CLOSED"){
        return <span className="text-sm text-muted-foreground">締切済み</span>;
    }

    return(
        <div className="flex flex-col items-end gap-1">
            {status === "DRAFT" &&(
                <Button size="sm" onClick={() => change("OPEN")} disabled={pending}>
                    公開
                </Button>
            )}

            {status === "OPEN" &&(
                <Button size="sm" variant="outline" onClick={() => change("CLOSED")} disabled={pending}>
                    締め切る
                </Button>
            )}

            {error && <span className="text-xs text-destructive">{error}</span>}
        </div>
    )
}