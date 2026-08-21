import { StatusBadge } from "@/components/event/statusBadge";
import StatusChangeDialog from "@/components/event/statusChangeDialog";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { EventDetailResponse } from "@/types/event/event";
import { cookies } from "next/headers";
import Link from "next/link";
import { notFound } from "next/navigation";

export default async function detailPage({
    params,
}:{
    params: Promise<{ id: string}>
}){

    const { id } = await params;
    const cookieStore = await cookies();

    const res = await fetch (`${process.env.API_BASE_URL}/api/events/${id}`, {
            headers: { Cookie : cookieStore.toString() },
            cache: "no-store",
        });

    if (res.status === 404 || res.status === 403) 
        notFound();
    
    //開発者向けのステータス発行
    if (!res.ok) 
        throw new Error(`イベント情報の取得に失敗しました: ${res.status}`);

    const event: EventDetailResponse = await res.json();

    return(
        <div>
            <Link href="/dashboard">
                ← ダッシュボード
            </Link>

            {/*イベント詳細の説明*/}
            <Card>
                <CardHeader>
                    <StatusBadge status={event.status}/>
                    <CardTitle>{event.title}</CardTitle>
                    <CardDescription>
                        作成日時：{event.createdAt}
                    </CardDescription>
                </CardHeader>
                <CardContent>
                    <p>質問数</p>
                    <p>{event.questionCount}</p>
                </CardContent>
            </Card>

            {/*参加者招待画面 */}
            <Card>
                <CardHeader>
                    <CardTitle>参加者を招待</CardTitle>
                </CardHeader>
                <CardContent>
                    <p>参加コード:{event.joinCode}</p>
                    <p>共有用参加URL:{`http://localhost:3000/join/${event.joinCode}`}</p>
                    <p>主催者用参加URL:{`http://localhost:3000/dashboard/${event.id}/qa`}</p>
                </CardContent>
            </Card>

            {/*ステータス更新画面*/}

            <Card>
                <CardHeader>
                    <CardTitle>受付の操作</CardTitle>
                    <CardDescription>
                        締め切ると参加者は投稿できなくなります。
                    </CardDescription>
                    <StatusChangeDialog
                        eventId={event.id}
                        title={event.title}
                        status={event.status}
                    />
                </CardHeader>
            </Card>
        </div>
    );
}