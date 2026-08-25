import { QuestionResponse, Sort } from "@/types/question/question";

export async function fetchGetQuestion(eventId: number, sort:Sort){

        const res = await fetch(`/api/events/${eventId}/questions?sort=${sort}`,{
             credentials: "same-origin", 
             headers: { Accept: "application/json" }
        });

        if(!res.ok){
            const problem = await res.json().catch(() => null);
            throw new Error(problem?.message??"エラーが発生しました");
        }

        const data:QuestionResponse[] = await res.json();

        return data;

}