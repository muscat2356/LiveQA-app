"use client"

import { JoinEvent } from "@/types/event/event";
import { useState } from "react";
import type { QuestionFilter, Sort } from "@/types/question/question";
import { Button } from "@/components/ui/button";
import useQuestions from "../../../../../components/question/useGuestQuestions";
import { StatusBadge } from "@/components/event/statusBadge";
import PostForm from "./PostForm";
import VoteButton from "./VoteButton";
import SortTab from "../../../../../components/question/SortTab";
import usePoll from "@/components/poll/usePoll";
import GuestPollCard from "@/components/guest/GuestPollCard";
import StatusTab from "@/components/question/StatusTab";
import { QuestionStatusBadge } from "@/components/question/questionStatusBadge";


export default function QaScreen({ event, nickname }: { event: JoinEvent; nickname: string}) {

    const [sort, setSort] = useState<Sort>("popular");
    const [ filter, setFilter ] = useState<QuestionFilter>("ALL");
    const {questions, loading, error, guestLoad, applyVote} = useQuestions(event.eventId, sort, filter);
    const { poll: polls, pending: pollPending, error: pollError, load: pollLoad } = usePoll(event.eventId);

    return(
        <div className="app-container max-w-2xl py-6">
            <header className="mb-4 flex items-start justify-between gap-3">
                <div className="min-w-0 space-y-2">
                    <h1 className="truncate text-xl font-bold tracking-tight sm:text-2xl">{event.title}</h1>
                    <StatusBadge status={event.status} />
                </div>
                <Button variant="outline" size="sm" onClick={() => { guestLoad(); pollLoad(); }} className="shrink-0">
                    {!error ? "更新" : "再更新"}
                </Button>
            </header>

            <div className="mb-4 flex flex-wrap items-center gap-2">
                <SortTab value={sort} onChange={setSort} />
                <StatusTab
                    value={filter}
                    onChange={setFilter}
                    isHost={false}
                />
            </div>

            {pollPending && polls.length === 0 && (
                <p className="py-6 text-center text-sm text-muted-foreground">読み込み中・・・</p>
            )}

            {!pollPending && pollError && (
                <p className="mb-4 rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">{pollError}</p>
            )}

            <div className="mb-4 space-y-3">
                {polls.map((p) => (
                    <GuestPollCard key={p.id} poll={p} load={pollLoad} />
                ))}
            </div>

            {!loading && error && <p>{error}</p>}

            {loading && <p>読み込み中・・・</p>}

            {!loading && !error &&questions.length === 0 && (
                <p>まだ質問はありません。最初の質問を投稿してみましょう！</p>
            )}

            <ul className="space-y-3">
                {questions.map((q) => (
                    <li key={q.id} className="rounded-2xl border bg-card p-4 shadow-sm ring-1 ring-foreground/5">
                        <p className="whitespace-pre-wrap break-words text-[15px] leading-relaxed">{q.body}</p>
                        <div className="mt-3 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
                            <span>{q.displayName ?? "匿名"}</span>
                            <span aria-hidden className="text-muted-foreground/40">·</span>
                            <VoteButton
                                questionId={q.id}
                                upvoteCount={q.upvoteCount}
                                votedByMe={q.votedByMe}
                                applyVote={applyVote}
                                onSync={guestLoad}
                            />
                            <QuestionStatusBadge status={q.status} />
                        </div>
                    </li>
                ))}
            </ul>

            <div className="mt-6">
                <PostForm eventId={event.eventId} nickname={nickname} onPosted={guestLoad} />
            </div>
        </div>
    )
}