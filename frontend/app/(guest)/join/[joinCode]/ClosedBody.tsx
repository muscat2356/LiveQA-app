import { Button } from "@/components/ui/button";
import Link from "next/link";

export function ClosedBody() {
  return (
    <div>
      <div>
        <p>受付は終了しました</p>
        <p>このイベントは締め切られました</p>
      </div>
        <Link href="/join">別のコードを入力する</Link>
    </div>
  );
}