"use client"

import { Button } from "@base-ui/react";
import { toast } from "sonner";

export default function CopyButton({text}: {text: string}){

    const handleCopy = async() => {
        try{
            await navigator.clipboard.writeText(text);
            toast.success("URLをコピーしました")
        }catch{
            toast.error("コピーに失敗しました")
        }
    };

    return(
        <Button onClick={handleCopy}>
            コピー
        </Button>

    )
}