"use client"

import { option } from "@/types/poll/poll";
import { Button } from "../ui/button";

export default function PollOptionSelect({
    options,
    isSelected,
    onToggle,
    disabled
}:{
    options: option[];
    isSelected: (optionId: number) => boolean;
    onToggle: (optionId: number) => void;
    disabled: boolean;
}){
    return(
        <div className="flex flex-col gap-2">
            {options.map((o) =>
                <Button
                    key={o.id}
                    type="button"
                    variant={isSelected(o.id) ? "default" : "outline"}
                    onClick={() => onToggle(o.id)}
                    disabled={disabled}
                    className="h-auto w-full justify-start whitespace-normal py-2.5 text-left"
                >
                    {o.label}
                </Button>
            )}
        </div>

    )
}