
import { QuestionResponse } from "@/types/question/question";
import { cookies } from "next/headers";
import { notFound } from "next/navigation";
import HostQaScreen from "./HostQaScreen";

export default async function HostQaPage({
    params,
}:{
    params: Promise<{ id : string}>
}){
    const { id } = await params;

    const cookieStore = await cookies();

    const res = await fetch (`${process.env.API_BASE_URL}/api/events/${id}s`, {
            headers: { Cookie : cookieStore.toString() },
            cache: "no-store",
    });

    if (res.status === 404 || res.status === 403) 
        notFound();

    if (!res.ok) {
        const problem = await res.json().catch(() => null);
        throw new Error(problem?.message ?? "再度実行してください");
    }

    const event = await res.json();

    return(
        <HostQaScreen event={event} />
    );
}