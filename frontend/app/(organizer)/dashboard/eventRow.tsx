"use client";

import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import {
  EventListResponse,
  statusLabel,
  statusVariant,
  nextStatusMap,
  actionLabelMap,
} from "@/types/event/event";

import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import Link from "next/link";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useRouter } from "next/navigation";
import { readCookie } from "@/components/cookie";

export default function EventRow({ event }: {event: EventListResponse }) {
    const router = useRouter();
    const [ open, setOpen ] = useState(false);
    const [ pending, setPending ] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const status = event.status;
    const nextStatus = nextStatusMap[status];
    const actionLabel = actionLabelMap[status];

    async function handleConfirm(){
        setPending(true)
        setErrorMessage(null);
        try{
            const xsrfToken = readCookie("XSRF-TOKEN");
            const res = await fetch(`/api/events/${event.id}/status`, {
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
                throw new Error(problem?.detail ?? `HTTP ${res.status}`);
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
        <Card>
            <CardContent>
                <div>
                    <Link href={`/dashboard/${event.id}`}>
                        {event.title}
                    </Link>
                    <p>
                        参加コード： {event.joinCode}
                    </p>
                    <p>
                        作成日時： {event.createdAt}
                    </p>
                </div>

                <div>
                    <Badge variant={statusVariant[status]}>{statusLabel[status]}</Badge>

                    <Dialog open={open} onOpenChange={setOpen}>
                        <DialogTrigger render={<Button variant="outline" size="sm"/>}>
                            {actionLabel}
                        </DialogTrigger>
                        <DialogContent>
                            <DialogHeader>
                                <DialogTitle>{event.title}</DialogTitle>
                                <DialogDescription>
                                    状態を「{statusLabel[nextStatus]}」に変更します。
                                </DialogDescription>
                            </DialogHeader>
                            {errorMessage && (
                                    <p>{errorMessage}</p>
                                )}
                            <DialogFooter>
                                <DialogClose render={<Button variant="outline" disabled={pending}/>}>
                                    キャンセル
                                </DialogClose>
                                <Button onClick={handleConfirm} disabled={pending}>
                                    {pending ? "送信中..." : actionLabel}
                                </Button>
                            </DialogFooter>
                        </DialogContent>
                    </Dialog>
                </div>
            </CardContent>
        </Card>
    );  
}
