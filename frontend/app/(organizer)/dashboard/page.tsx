import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EventListResponse } from "@/types/event/event";
import Link from "next/link";
import EventRow from "./eventRow";
import { cookies } from "next/headers";

export default async function DashBoard(){
    
    const cookieStore = await cookies();

    const res =await fetch (`${process.env.API_BASE_URL}/api/events`, {
        headers: { Cookie : cookieStore.toString() },
        cache: "no-store",
    });

    if(!res.ok){
        throw new Error(`API error: ${res.status}`);
    }

    const events: EventListResponse[] = await res.json();

    return (
        <div>
            <div>
                <h1>Event DashBoard</h1>
                <Button nativeButton={false} render={<Link href="/dashboard/new" />}>
                    新規作成
                </Button>
            </div>
            
            {events.length === 0 ? (
                <Card>
                    <CardHeader>
                        <CardTitle>最初のイベントを作成してください。</CardTitle>
                        <CardDescription>
                            イベントを作成するとコードが発行されます。
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <Button nativeButton={false} render={<Link href="/dashboard/new" />}>
                            イベント新規作成
                        </Button>
                    </CardContent>
                </Card>
            ) : (
                <div>
                    {events.map((event) => (
                        <EventRow key={event.id} event={event}/>
                    ))}
                </div>
            )}
        </div>
    );
}