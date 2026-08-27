"use client"

import { useState } from "react";
import usePollForm from "./usePollForm";
import useCreatePoll from "./useCreatePoll";
import { Button } from "../ui/button";

import { Textarea } from "../ui/textarea";
import { Label } from "../ui/label";
import PollTypeSelect from "./PollTypeSelect";
import PollOptionInputs from "./PollOptionInputs";

export default function PollCreateForm({
    eventId,
    load
}:{
    eventId: number;
    load: () => void;
}){

    const [ open, setOpen ] = useState(false);
    const form = usePollForm();
    const { handleSubmit, pending, error} = useCreatePoll(eventId);

    async function pollPost(){

        const request = await handleSubmit({
            questionText: form.trimmedText,
            type: form.pollType,
            options: form.labels
        })

        if(request){
            form.reset();
            setOpen(false);
            load();
        }
    }

    if(!open){
        return(
            <div>
                <Button variant="outline" onClick={() => setOpen(true)}>
                    +投票を作成
                </Button>
            </div>
        )
    }

    return(
        <div>
            <Label>設問</Label>
            
            <Textarea 
                value={form.questionText}
                onChange={(e) => form.setQuestionText(e.target.value)}
                maxLength={200}
                rows={2}
            />
            <p>{form.trimmedText.length}/200</p>

            <PollTypeSelect
                value={form.pollType} onChange={form.setPollType}
            />

            <PollOptionInputs
                options={form.options}
                onAdd={form.addOption}
                onEdit={form.editOption}
                onRemove={form.removeOption}
                hasDuplicate={form.hasDuplicate}
            />

            {error && (
                <p>{error}</p>
            )}

            <Button onClick={() => {form.reset(); setOpen(false);}} disabled={pending}>
                キャンセル
            </Button>

            <Button onClick={pollPost} disabled={form.invalid || pending}>
                {pending ? "作成中" : "作成"}
            </Button>

        </div>
    )
}