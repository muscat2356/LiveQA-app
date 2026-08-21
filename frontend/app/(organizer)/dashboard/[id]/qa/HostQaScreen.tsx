"use client"

import SortTab from "@/components/question/SortTab";
import useQuestions from "@/components/question/useQuestions";
import { StatusBadge } from "@/components/event/statusBadge";
import { Button } from "@/components/ui/button";
import { EventDetailResponse } from "@/types/event/event";
import { Sort } from "@/types/question/question";
import { useState } from "react";
import QuestionStatusButton from "./QuestionStatusButton";
import { QuestionStatusBadge } from "@/components/question/questionStatusBadge";

export default  function HostQaScreen({
     event 
} : { 
    event: EventDetailResponse
}){

    const [ sort, setSort ] = useState<Sort>("popular");
    const { questions, loading, error, load } = useQuestions(event.id, sort);
    
    return(
        <div>
            <h1>{event.title}</h1>
            <StatusBadge status={event.status}/>

            <Button onClick={load} disabled={loading}>
                更新
            </Button>

            <SortTab 
                value={sort}
                onChange={setSort}
            />

            {loading && <p>読み込み中・・・</p>}
            {!loading &&!error && questions.length === 0 && <p>まだ質問がありません</p>}
            {!loading &&error && questions.length === 0 && <p>まだ質問がありません</p>}
            
            <ul>
                {questions.map((q) => (
                    <li key={q.id}>
                        <QuestionStatusBadge status={q.status} />
                        <p>{q.body}</p>
                        <div>
                            {q.displayName ?? "匿名"}
                        </div>
                        <div>
                            いいね数:{q.upvoteCount}
                        </div>
                        <QuestionStatusButton 
                            questionId={q.id}
                            status={q.status}
                            load={load}
                        />
                    </li>
                ))}
            </ul>
        </div>
    );
}