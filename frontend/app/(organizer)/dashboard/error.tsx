"use client";

import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function Error({ reset }: { error: Error; reset: () => void }) {
    return (
        <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 px-4 text-center">
            <p className="text-muted-foreground">イベントの取得に失敗しました。</p>
            <button onClick={reset} className={cn(buttonVariants({ variant: "default", size: "lg" }))}>再試行</button>
        </div>
    );
}