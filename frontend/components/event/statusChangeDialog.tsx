"use client"

import { actionLabelMap, EventStatus, nextStatusMap, statusLabel } from "@/types/event/event";

import { useState } from "react";
import { readCookie } from "../cookie";
import { useRouter } from "next/navigation";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../ui/dialog";
import { Button } from "../ui/button";

export default function statusChangeDialog({
    eventId,
    title,
    status,
}: {
    eventId: number;
    title: string;
    status: EventStatus;    
}){
    const router = useRouter();
    const [ open, setOpen ] = useState(false);
    const [ pending, setPending ] = useState(false);
    const [ errorMessage, setErrorMessage] = useState<string | null>(null);

    const nextStatus = nextStatusMap[status];
    const actionLabel = actionLabelMap[status];

    async function handleConfirm(){
            setPending(true)
            setErrorMessage(null);
            try{
                const xsrfToken = readCookie("XSRF-TOKEN");
                const res = await fetch(`/api/events/${eventId}/status`, {
                    method: "PATCH",
                    headers:  { 
                        "Content-Type": "application/json",
                        ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
                    },
                    credentials: "same-origin",
                    body: JSON.stringify({ status: nextStatus }),
                });
    
                if(!res.ok){
                    // 本文にエラー情報が入っている場合はここで読む
                    const problem = await res.json().catch(() => null)
                    throw new Error(problem?.message ?? "エラーが発生しました");
                }
    
                setOpen(false);
                router.refresh();
            }catch (e) {
                console.error(e);
                setErrorMessage(e instanceof Error ? e.message : "エラーが発生しました");
            }finally{
                setPending(false);
            }
    }

    return(
        <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger render={<Button variant="outline" size="sm"/>}>
                {actionLabel}
            </DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>
                        状態を「{statusLabel[nextStatus]}」に変更します。
                    </DialogDescription>
                </DialogHeader>
                {nextStatus === "CLOSED" && (
                        <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">締め切ると質問はすべて削除されます。この操作は取り消せません。</p>
                    )}
                {errorMessage && (
                        <p className="text-sm text-destructive">{errorMessage}</p>
                    )}
                <DialogFooter>
                    <DialogClose render={<Button variant="outline" disabled={pending}/>}>
                        キャンセル
                    </DialogClose>
                    <Button variant={nextStatus === "CLOSED" ? "destructive" : "default"} onClick={handleConfirm} disabled={pending}>
                        {pending ? "送信中..." : actionLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}