"use client"

import { JoinEvent } from "@/types/event/event";
import { useState } from "react";
import type { Sort } from "@/types/question/question";
import { Button } from "@/components/ui/button";
import useQuestions from "./useQuestions";
import { StatusBadge } from "@/components/event/statusBadge";
import PostForm from "./PostForm";


export default function QaScreen({ event, nickname }: { event: JoinEvent; nickname: string}) {

    const [sort, setSort] = useState<Sort>("popular");
    const {questions, loading, load} = useQuestions(event.eventId, sort);

    return(
        <div>
            <h1>{event.title}</h1>
            <StatusBadge status={event.status} />
            <Button onClick={load}>更新</Button>

            {loading && <p>読み込み中・・・</p>}

            {!loading && questions.length === 0 && (
                <p>まだ質問はありません。最初の質問を投稿してみましょう！</p>
            )}

            <ul>
                {questions.map((q) => (
                    <li key={q.id}>
                        <p>{q.body}</p>
                        <p>
                            {q.displayName ?? "匿名"} / いいね{q.upvoteCount}/
                            {q.votedByMe ? "いいね" : "未選択"} / {q.status}
                        </p>
                    </li>
                ))}
            </ul>

            <PostForm eventId={event.eventId} nickname={nickname} onPosted={load} />
        </div>
    )
}