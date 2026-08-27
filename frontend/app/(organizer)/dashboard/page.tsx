import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EventListResponse } from "@/types/event/event";
import Link from "next/link";
import EventRow from "./eventRow";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import LogoutButton from "./LogoutButton";

export default async function DashBoard(){
    
    const cookieStore = await cookies();

    const res = await fetch (`${process.env.API_BASE_URL}/api/events`, {
        headers: { Cookie : cookieStore.toString() },
        cache: "no-store",
    });

    if (res.status === 401) {
        redirect("/");
    }

    if(!res.ok){
        throw new Error(`API error: ${res.status}`);
    }

    const events: EventListResponse[] = await res.json();

    return (
        <div className="app-container py-8 sm:py-12">
            <div className="mb-8 flex flex-wrap items-center justify-between gap-4">
                <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">Event DashBoard</h1>
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
