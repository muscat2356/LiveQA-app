"use client"

import { JoinEvent } from "@/types/event/event";
import { useState } from "react";
import type { Sort } from "@/types/question/question";
import { Button } from "@/components/ui/button";
import useQuestions from "./useQuestions";
import { StatusBadge } from "@/components/event/statusBadge";
import PostForm from "./PostForm";
import VoteButton from "./VoteButton";
import SortTab from "./SortTab";


export default function QaScreen({ event, nickname }: { event: JoinEvent; nickname: string}) {

    const [sort, setSort] = useState<Sort>("popular");
    const {questions, loading, error, load, applyVote} = useQuestions(event.eventId, sort);

    return(
        <div className="app-container max-w-2xl py-6">
            <header className="mb-4 flex items-start justify-between gap-3">
                <div className="min-w-0 space-y-2">
                    <h1 className="truncate text-xl font-bold tracking-tight sm:text-2xl">{event.title}</h1>
                    <StatusBadge status={event.status} />
                </div>
                <Button variant="outline" size="sm" onClick={load} className="shrink-0">
                    {!error ? "更新" : "再更新"}
                </Button>
            </header>

            <div className="mb-4">
                <SortTab value={sort} onChange={setSort} />
            </div>

            {!loading && error && <p className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</p>}

            {loading && <p className="py-10 text-center text-sm text-muted-foreground">読み込み中・・・</p>}

            {!loading && !error &&questions.length === 0 && (
                <p className="py-10 text-center text-sm text-muted-foreground">まだ質問はありません。最初の質問を投稿してみましょう！</p>
            )}

            <ul className="space-y-3">
                {questions.map((q) => (
                    <li key={q.id} className="rounded-2xl border bg-card p-4 shadow-sm ring-1 ring-foreground/5">
                        <p className="whitespace-pre-wrap break-words text-[15px] leading-relaxed">{q.body}</p>
                        <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
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

            <div className="mt-6">
                <PostForm eventId={event.eventId} nickname={nickname} onPosted={load} />
            </div>
        </div>
    )
}