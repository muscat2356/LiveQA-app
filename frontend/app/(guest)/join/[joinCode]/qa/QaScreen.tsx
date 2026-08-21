"use client"

import { JoinEvent } from "@/types/event/event";
import { useState } from "react";
import type { Sort } from "@/types/question/question";
import { Button } from "@/components/ui/button";
import useQuestions from "../../../../../components/question/useQuestions";
import { StatusBadge } from "@/components/event/statusBadge";
import PostForm from "./PostForm";
import VoteButton from "./VoteButton";
import SortTab from "../../../../../components/question/SortTab";


export default function QaScreen({ event, nickname }: { event: JoinEvent; nickname: string}) {

    const [sort, setSort] = useState<Sort>("popular");
    const {questions, loading, error, load, applyVote} = useQuestions(event.eventId, sort);

    return(
        <div>
            <h1>{event.title}</h1>
            <StatusBadge status={event.status} />
            <Button onClick={load}>
                {!error ? "更新" : "再更新"}
            </Button>
            {!loading && error && <p>{error}</p>}

            {loading && <p>読み込み中・・・</p>}

            {!loading && !error &&questions.length === 0 && (
                <p>まだ質問はありません。最初の質問を投稿してみましょう！</p>
            )}

            <SortTab value={sort} onChange={setSort} />

            <ul>
                {questions.map((q) => (
                    <li key={q.id}>
                        <p>{q.body}</p>
                        <div>
                            {q.displayName ?? "匿名"} / 
                            <VoteButton 
                                questionId={q.id}
                                upvoteCount={q.upvoteCount}
                                votedByMe={q.votedByMe}
                                applyVote={applyVote}
                                onSync={load}
                            />/ 
                            {q.status}
                        </div>
                    </li>
                ))}
            </ul>

            <PostForm eventId={event.eventId} nickname={nickname} onPosted={load} />
        </div>
    )
}