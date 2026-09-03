"use client"

import { useState } from "react";
import usePollForm from "../../features/poll/usePollForm";
import useCreatePoll from "../../features/poll/useCreatePoll";
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
            <div className="mt-6">
                <Button variant="outline" onClick={() => setOpen(true)} className="w-full">
                    +投票を作成
                </Button>
            </div>
        )
    }

    return(
        <div className="mt-6 space-y-4 rounded-2xl border bg-card p-4 shadow-sm ring-1 ring-foreground/5">
            <div className="space-y-1.5">
                <Label>設問</Label>
                <Textarea
                    value={form.questionText}
                    onChange={(e) => form.setQuestionText(e.target.value)}
                    maxLength={200}
                    rows={2}
                    className="resize-none"
                />
                <p className="text-right text-xs text-muted-foreground">{form.trimmedText.length}/200</p>
            </div>

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
                <p className="text-sm text-destructive">{error}</p>
            )}

            <div className="flex justify-end gap-2 border-t pt-4">
                <Button variant="outline" onClick={() => {form.reset(); setOpen(false);}} disabled={pending}>
                    キャンセル
                </Button>

                <Button onClick={pollPost} disabled={form.invalid || pending}>
                    {pending ? "作成中" : "作成"}
                </Button>
            </div>

        </div>
    )
}