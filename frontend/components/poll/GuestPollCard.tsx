"use client"

import { PollDto } from "@/types/poll/poll";
import usePollAnswer from "../../features/poll/useSelectPoll";
import useAnswerPoll from "../../features/poll/useSubmitPollAnswer";
import { PollResultChart } from "./PollResultChart";
import PollOptionSelect from "./PollOptionSelect";
import { Button } from "../ui/button";

export default function GuestPollCard({
    poll,
    load,
}:{
    poll:PollDto;
    load:() => void;
}){

    const form = usePollAnswer(poll.type);
    const answer = useAnswerPoll(poll.id);

    async function post(){

        const ok = await answer.handleSubmit(form.selectedIds);
            
        if(ok){
            form.reset();
            load();
        }
    } 

    const answered = poll.myOptionIds.length > 0;
    const closed = poll.status === "CLOSED";

    return(
        <div className="rounded-2xl border bg-card p-4 shadow-sm ring-1 ring-primary/10">
            <div className="mb-3 space-y-1.5">
                <span className="inline-flex w-fit items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">投票</span>
                <p className="text-[15px] font-semibold leading-snug">{poll.questionText}</p>
                <p className="text-xs text-muted-foreground">
                    {poll.type === "SINGLE" ? "単一選択" : "複数選択"} / 回答 {poll.totalResponses} 件
                </p>
            </div>

            {answered || closed ?(
                <div className="space-y-2">
                    {answered && !closed && (
                        <p className="text-sm font-medium text-primary">回答済みです</p>
                    )}
                    {closed &&(
                        <p className="text-sm text-muted-foreground">この投票は締め切られました</p>
                    )}
                    <PollResultChart poll={poll}/>
                </div>
            ):(
                <div className="space-y-3">
                    <PollOptionSelect
                        options={poll.options}
                        isSelected={form.isSelected}
                        onToggle={form.toggle}
                        disabled={answer.pending}
                    />

                    {answer.error &&(
                        <p className="text-sm text-destructive">
                            {answer.error}
                        </p>
                    )}

                    <Button
                        onClick={post}
                        disabled={form.invalid || answer.pending}
                        className="w-full"
                    >
                        {answer.pending ? "送信中・・・" : "回答する"}
                    </Button>
                </div>
            )}
        </div>
    );
}