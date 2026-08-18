import { StatusBadge } from "@/components/event/statusBadge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { fetchJoinEvent } from "@/lib/api/eventApi";
import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import EnterForm from "./EnterForm";
import { ClosedBody } from "./ClosedBody";
import { WaitingBody } from "./WaitingBody";

export default async function JoinLanding({
    params,
}:{
    params: Promise<{ joinCode: string}>;
}){
    const { joinCode } = await params;

    const event = await fetchJoinEvent(joinCode);
    if(!event) notFound();

    const saveNickName = (await cookies()).get("qa_nickname")?.value ?? "";

    return(
        <div>
            <Card>
                <CardHeader>
                    <StatusBadge status={event.status} />
                    <CardTitle></CardTitle>
                </CardHeader>
                <CardContent>
                    {event.status === "DRAFT" && <WaitingBody />}
                    {event.status === "OPEN" && (<EnterForm joinCode={joinCode} initialNickname={saveNickName} />)}
                    {event.status === "CLOSED" && (<ClosedBody />)}
                </CardContent>
            </Card>
        </div>
    );
}