"use client"

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";

export function WaitingBody(){
    const router = useRouter();

    return(
        <div>
            <div>
                <p>まもなく開始します</p>
                <p>
                    開始されると質問を投稿できます。
                </p>
            </div>
            <Button variant="outline" onClick={() => router.refresh()}>
                最新の状態に更新
            </Button>
        </div>
    )
}