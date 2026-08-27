"use client"

import { QuestionFilter, questionFilterLabel } from "@/types/question/question";
import { Tabs, TabsList, TabsTrigger } from "../ui/tabs";

export default function StatusTab({
    value,
    onChange,
    isHost,
}:{
    value: QuestionFilter;
    onChange:(value: QuestionFilter) => void;
    isHost: boolean;
}){
    return(
        <div>
            <Tabs value={value} onValueChange={(v) => onChange(v as QuestionFilter)}>
                <TabsList>
                    {(["ALL", "OPEN", "ANSWERED", "ARCHIVED" ] as const)
                    .filter((f) => isHost || f !== "ARCHIVED")
                    .map((f) => (
                        <TabsTrigger key={f} value={f}>{questionFilterLabel[f]}</TabsTrigger>
                    ))}
                </TabsList>
            </Tabs>
        </div>
    )
}