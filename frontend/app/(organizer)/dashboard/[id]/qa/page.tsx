import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import HostQaScreen from "./HostQaScreen";
import { fetchGetEventDetail } from "@/lib/api/server/eventApi";
import { EventDetailResponse } from "@/types/event/event";

export default async function HostQaPage({
    params,
}:{
    params: Promise<{ id : string}>
}){
    const { id } = await params;

    const event:EventDetailResponse | null = await fetchGetEventDetail(id);

    if(!event){
        throw new Error ("エラーが発生");
    }

    return(
        <HostQaScreen event={event} />
    );
}