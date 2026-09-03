import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EventListResponse } from "@/types/event/event";
import Link from "next/link";
import EventRow from "./EventRow";
import { redirect } from "next/navigation";
import LogoutButton from "./LogoutButton";
import { fetchGetEvent } from "@/lib/api/server/eventApi";

/**
 * 主催者イベントダッシュボード画面
 * 該当ユーザーのイベント全件表示
 * @returns 
 */
export default async function DashBoard(){

    const events:EventListResponse[]| null = await fetchGetEvent();

    if (!events) {
        redirect("/");
    }

    return (
        <div className="app-container py-8 sm:py-12">
            <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
                <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">イベント一覧</h1>
                <div className="flex items-center gap-2">
                    <Button size="lg" nativeButton={false} render={<Link href="/dashboard/new" />}>
                        新規作成
                    </Button>
                    <LogoutButton />
                </div>
            </div>

            {events.length === 0 ? (
                <Card className="mx-auto max-w-md text-center">
                    <CardHeader className="items-center">
                        <CardTitle className="text-lg">最初のイベントを作成してください。</CardTitle>
                        <CardDescription>
                            イベントを作成するとコードが発行されます。
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="flex justify-center">
                        <Button size="lg" nativeButton={false} render={<Link href="/dashboard/new" />}>
                            イベント新規作成
                        </Button>
                    </CardContent>
                </Card>
            ) : (
                <div className="grid gap-4 sm:grid-cols-2">
                    {events.map((event) => (
                        <EventRow key={event.id} event={event}/>
                    ))}
                </div>
            )}
        </div>
    );
}
