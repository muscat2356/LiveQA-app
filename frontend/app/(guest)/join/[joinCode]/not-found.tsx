import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import Link from "next/link";

export default function NotFound(){
    return(
        <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4 py-10 text-center">
            <h1 className="text-2xl font-bold tracking-tight">該当のルームが存在しません。</h1>
            <p className="max-w-sm text-muted-foreground">参加コードが間違っているか、イベントが削除されています</p>
            <Link href="/join" className={cn(buttonVariants({ variant: "default", size: "lg" }))}>
                コードを入力し直す
            </Link>
        </div>
    );
}