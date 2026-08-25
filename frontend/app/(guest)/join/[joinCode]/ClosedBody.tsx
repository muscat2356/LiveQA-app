import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils/utils";
import Link from "next/link";

export function ClosedBody() {
  return (
    <div className="space-y-4 text-center">
      <div className="space-y-1">
        <p className="text-base font-semibold">受付は終了しました</p>
        <p className="text-sm text-muted-foreground">このイベントは締め切られました</p>
      </div>
        <Link href="/join" className={cn(buttonVariants({ variant: "outline", size: "lg" }), "w-full")}>別のコードを入力する</Link>
    </div>
  );
}