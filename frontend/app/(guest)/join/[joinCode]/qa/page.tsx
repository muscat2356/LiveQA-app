import { fetchJoinEvent } from "@/lib/api/eventApi";
import { notFound, redirect } from "next/navigation";
import QaScreen from "./QaScreen";

export default async function QaPage({
    params,
}: {
    params: Promise<{ joinCode: string}>;
}){
    const { joinCode } = await params;

    const event = await fetchJoinEvent(joinCode);

    if(!event) notFound();
    if(event.status !== "OPEN") redirect(`/join/${joinCode}`);

    return (
        <QaScreen event={event} />
    );
}