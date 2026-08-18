import Link from "next/link";

export default function NotFound(){
    return(
        <div>
            <h1>該当のルームが存在しません。</h1>
            <p>参加コードが間違っているか、イベントが削除されています</p>
            <Link href="/join">
                コードを入力し直す
            </Link>
        </div>
    );
}