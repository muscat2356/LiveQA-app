"use client"

import { Button } from "@/components/ui/button";
import { RiFileCopyLine } from "@remixicon/react";
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
        <Button type="button" variant="outline" size="sm" onClick={handleCopy}>
            <RiFileCopyLine className="size-4" />
            コピー
        </Button>

    )
}
