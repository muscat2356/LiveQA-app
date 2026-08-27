"use client"

import { PollDto } from "@/types/poll/poll";
import usePollAnswer from "../poll/usePollAnswer";
import useAnswerPoll from "./useAnswerPoll";
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
        <div>
            <p>{poll.questionText}</p>
            <p>
                {poll.type === "SINGLE" ? "単一選択" : "複数選択"} / 回答 {poll.totalResponses} 件
            </p>

            {answered || closed ?(
                <div>
                    {answered && !closed && (
                        <p>回答済みです</p>
                    )}
                    {closed &&(
                        <p>この投票は締め切られました</p>
                    )}
                    <PollResultChart poll={poll}/>
                </div>
            ):(
                <div>
                    <PollOptionSelect
                        options={poll.options}
                        isSelected={form.isSelected}
                        onToggle={form.toggle}
                        disabled={answer.pending}
                    />

                    {answer.error &&(
                        <p>
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