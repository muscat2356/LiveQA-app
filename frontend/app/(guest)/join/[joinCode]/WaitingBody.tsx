"use client"

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

export function WaitingBody(){
    const router = useRouter();

    return(
        <div className="space-y-4 text-center">
            <div className="space-y-1">
                <p className="text-base font-semibold">まもなく開始します</p>
                <p className="text-sm text-muted-foreground">
                    開始されると質問を投稿できます。
                </p>
            </div>
            <Button variant="outline" onClick={() => router.refresh()} className="w-full">
                最新の状態に更新
            </Button>
        </div>
    )
}