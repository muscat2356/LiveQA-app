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
        <div className="app-container max-w-3xl space-y-6 py-8 sm:py-10">
            <Link
                href="/dashboard"
                className="inline-flex items-center text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
                ← ダッシュボード
            </Link>

            {/*イベント詳細の説明*/}
            <Card>
                <CardHeader className="gap-2">
                    <StatusBadge status={event.status}/>
                    <CardTitle className="text-2xl">{event.title}</CardTitle>
                    <CardDescription>
                        作成日時：{event.createdAt}
                    </CardDescription>
                </CardHeader>
                <CardContent className="flex items-baseline gap-2">
                    <p className="text-sm text-muted-foreground">質問数</p>
                    <p className="text-3xl font-bold tabular-nums">{event.questionCount}</p>
                </CardContent>
            </Card>

            <div className="space-y-6">
                {/*参加者招待画面 */}
                <Card>
                    <CardHeader>
                        <CardTitle>参加者を招待</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3 text-sm">
                        <p className="flex flex-wrap items-baseline gap-x-2">
                            <span className="text-muted-foreground">参加コード:</span>
                            <span className="font-mono text-base font-semibold tracking-wider">{event.joinCode}</span>
                        </p>
                        <p className="flex flex-wrap items-baseline gap-x-2">
                            <span className="text-muted-foreground">参加者URL:</span>
                            <span className="break-all font-mono text-primary">{`http://localhost:3000/join/${event.joinCode}`}</span>
                        </p>
                        <p className="flex flex-wrap items-baseline gap-x-2">
                            <span className="text-muted-foreground">主催者URL:</span>
                            <span className="break-all font-mono text-primary">{`http://localhost:3000/join/${event.id}/qa`}</span>
                        </p>
                    </CardContent>
                </Card>

                {/*ステータス更新画面*/}
                <Card>
                    <CardHeader className="gap-3">
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
        </div>
    );
}