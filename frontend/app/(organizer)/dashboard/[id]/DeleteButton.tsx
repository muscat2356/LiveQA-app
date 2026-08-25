"use client"

import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { deleteEvent } from "@/lib/api/eventApi"

import { useRouter } from "next/navigation";
import { useState } from "react";

export default function DeleteButton({ eventId }: { eventId: number }){

    const [ pending, setPending ] = useState(false);
    const [ errorMessage, setErrorMessage ] = useState("");
    const [ open, setOpen ] = useState(false);
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

        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger render={<Button variant="outline" size="sm"/>}>
                {pending ? "削除中" : "削除"}
            </DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>イベント削除</DialogTitle>
                    <DialogDescription>
                        イベントを削除しますか？
                    </DialogDescription>
                </DialogHeader>
                {errorMessage && (
                    <p className="text-sm text-destructive">{errorMessage}</p>
                )}
                <DialogFooter>
                    <DialogClose render={<Button variant="outline" disabled={pending}/>}>
                        キャンセル
                    </DialogClose>
                    <Button variant="destructive" onClick={handleDelete} disabled={pending}>
                        {pending ? "削除中・・・" : "削除"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}