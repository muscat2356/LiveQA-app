"use client"

import { PollType } from "@/types/poll/poll";
import { useState } from "react";

export default function usePollAnswer(type: PollType){

    const [ selectedIds, setSelectedIds] = useState<number[]>([]);

    const toggle = (optionId: number) => {
        if(type === "SINGLE"){
            setSelectedIds([optionId]);
        }else if(type === "MULTI"){
            setSelectedIds((prev) => 
                prev.includes(optionId) ? prev.filter((o) => o !== optionId):[...prev, optionId]);
        }
    }

    const invalid = selectedIds.length === 0;

    const isSelected = (optionId: number) => {
        return selectedIds.includes(optionId);
    }

    const reset = () => {
        setSelectedIds([]);
    }

    return {
        selectedIds, setSelectedIds,
        toggle,
        invalid,
        isSelected,
        reset
    }
}