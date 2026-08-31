import CopyButton from "@/components/event/CopyButton";
import { StatusBadge } from "@/components/event/statusBadge";
import StatusChangeDialog from "@/components/event/statusChangeDialog";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EventDetailResponse } from "@/types/event/event";
import { cookies } from "next/headers";
import Link from "next/link";
import { notFound } from "next/navigation";
import UpdateEventNameForm from "./UpdateEventNameForm";
import DeleteButton from "./DeleteButton";
import formDateTime from "@/lib/utils/date";

export default async function detailPage({
    params,
}:{
    params: Promise<{ id: string}>
}){

    const { id } = await params;
    const cookieStore = await cookies();

    const baseURL = process.env.NEXT_PUBLIC_URL ?? "http://localhost:3000";

    const hostUrlpath = `${baseURL}/dashboard/${id}/qa`;

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

    const participantUrl = baseURL + "/join/" + event.joinCode;

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
                        作成日時：{formDateTime(event.createdAt)}
                    </CardDescription>
                </CardHeader>
                <CardContent className="space-y-5">
                    <div className="flex items-baseline gap-2">
                        <p className="text-sm text-muted-foreground">質問数</p>
                        <p className="text-3xl font-bold tabular-nums">{event.questionCount}</p>
                    </div>

                    <div className="border-t pt-5">
                        <UpdateEventNameForm
                            eventId={event.id}
                            cuurentName={event.title}
                        />
                    </div>

                    <div className="flex items-center justify-between gap-3 border-t pt-5">
                        <p className="text-sm text-muted-foreground">このイベントを削除します</p>
                        <DeleteButton
                            eventId={event.id}
                        />
                    </div>
                </CardContent>
            </Card>

            <div className="space-y-6">
                {/*参加者招待画面 */}
                <Card>
                    <CardHeader>
                        <CardTitle>参加者を招待</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4 text-sm">
                        <div className="space-y-1.5">
                            <span className="text-xs font-medium text-muted-foreground">参加コード:</span>
                            <div className="rounded-lg bg-muted px-3 py-2 font-mono text-base font-semibold tracking-wider">{event.joinCode}</div>
                        </div>
                        <div className="space-y-1.5">
                            <span className="text-xs font-medium text-muted-foreground">参加者URL:</span>
                            <div className="break-all rounded-lg bg-muted px-3 py-2 font-mono text-primary">{participantUrl}</div>
                            <CopyButton
                                text={participantUrl}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <span className="text-xs font-medium text-muted-foreground">主催者URL:</span>
                            <Link
                                href={`/dashboard/${event.id}/qa`}
                                className="block break-all rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 font-mono text-primary transition-colors hover:bg-primary/10 hover:underline"
                            >
                                {hostUrlpath}
                            </Link>
                            <CopyButton
                                text={hostUrlpath}
                            />                  
                        </div>
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
