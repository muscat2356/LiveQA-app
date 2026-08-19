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
            <Card>
                <CardHeader>
                    <CardTitle>イベント詳細</CardTitle>
                    <CardDescription></CardDescription>
                </CardHeader>
                <CardContent>
                    <p></p>
                    <p></p>
                    <Link href={""}>
                    </Link>
                </CardContent>
                <CardFooter>
                    <StatusChangeDialog
                        eventId={event.id}
                        title={event.title}
                        status={event.status}
                    />
                </CardFooter>
            </Card>
        </div>
    )
}