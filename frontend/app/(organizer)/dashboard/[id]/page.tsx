import { EventDetailResponse } from "@/types/event/event";
import { cookies } from "next/headers";
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
                
            </Card>

        </div>
    )
}