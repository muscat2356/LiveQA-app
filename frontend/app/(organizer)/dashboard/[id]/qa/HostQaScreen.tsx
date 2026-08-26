"use client"

import SortTab from "@/components/question/SortTab";
import { StatusBadge } from "@/components/event/statusBadge";
import { Button } from "@/components/ui/button";
import { EventDetailResponse } from "@/types/event/event";
import { Sort } from "@/types/question/question";
import { useState } from "react";
import QuestionStatusButton from "./QuestionStatusButton";
import { QuestionStatusBadge } from "@/components/question/questionStatusBadge";
import useHostQuestions from "@/components/question/useHostQuestions";
import { OrganizerChart } from "@/components/poll/organizerChart";
import usePoll from "@/components/poll/usePoll";
import PollCreateForm from "@/components/poll/PollCreateForm";

export default  function HostQaScreen({
     event 
} : { 
    event: EventDetailResponse
}){

    const [ sort, setSort ] = useState<Sort>("popular");
    const { questions, loading, error, hostLoad } = useHostQuestions(event.id, sort);
    const { poll, pending: pollPending, error: pollError, load: pollLoad } = usePoll(event.id);

    return(
        <div className="app-container max-w-2xl py-6">
            <header className="mb-4 flex items-start justify-between gap-3">
                <div className="min-w-0 space-y-2">
                    <h1 className="truncate text-xl font-bold tracking-tight sm:text-2xl">{event.title}</h1>
                    <StatusBadge status={event.status}/>
                </div>
                <Button variant="outline" size="sm" onClick={() => {hostLoad(); pollLoad();}} disabled={loading || pollPending} className="shrink-0">
                    更新
                </Button>
            </header>

            <div className="mb-4">
                <SortTab
                    value={sort}
                    onChange={setSort}
                />
            </div>

            {pollPending && poll.length === 0 && (
                <p className="py-6 text-center text-sm text-muted-foreground">読み込み中・・・</p>
            )}

            {!pollPending && pollError && (
                <p className="mb-4 rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">{pollError}</p>
            )}

            <div className={`space-y-4 ${pollPending ? "opacity-60" : ""}`}>
                {poll.map((p) => (
                    <OrganizerChart
                        key={p.id}
                        pending={pollPending}
                        error={pollError}
                        poll={p}
                        onChanged={pollLoad}
                    />
                ))}
            </div>
            
            {loading && <p className="py-10 text-center text-sm text-muted-foreground">読み込み中・・・</p>}
            {!loading &&!error && questions.length === 0 && <p className="py-10 text-center text-sm text-muted-foreground">まだ質問がありません</p>}
            {!loading &&error && <p className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">再度実行してください</p>}

            <ul className="space-y-3">
                {questions.map((q) => (
                    <li key={q.id} className="rounded-2xl border bg-card p-4 shadow-sm ring-1 ring-foreground/5">
                        <QuestionStatusBadge status={q.status} />
                        <p className="mt-2 whitespace-pre-wrap break-words text-[15px] leading-relaxed">{q.body}</p>
                        <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted-foreground">
                            <div>
                                {q.displayName ?? "匿名"}
                            </div>
                            <div>
                                いいね数:{q.upvoteCount}
                            </div>
                        </div>
                        <QuestionStatusButton
                            questionId={q.id}
                            status={q.status}
                            load={hostLoad}
                        />
                    </li>
                ))}
            </ul>
            <PollCreateForm eventId={event.id} load={pollLoad} />
        </div>
    );
}