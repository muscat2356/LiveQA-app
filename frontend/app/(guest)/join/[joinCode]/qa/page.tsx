import { fetchJoinEvent } from "@/lib/api/server/eventApi";
import { notFound, redirect } from "next/navigation";
import QaScreen from "./QaScreen";
import { cookies } from "next/headers";

export default async function QaPage({
    params,
}: {
    params: Promise<{ joinCode: string}>;
}){
    const { joinCode } = await params;

    const event = await fetchJoinEvent(joinCode);
    const nickname = (await cookies()).get("qa_nickname")?.value ?? "";

    if(!event) notFound();
    if(event.status !== "OPEN") redirect(`/join/${joinCode}`);

    return (
        <QaScreen event={event} nickname={nickname}/>
    );
}