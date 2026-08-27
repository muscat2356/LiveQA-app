"use client"

import { PollType } from "@/types/poll/poll"
import { Label } from "../ui/label";
import { Button } from "../ui/button";

export default function PollTypeSelect({
    value,
    onChange
}:{
    value: PollType;
    onChange: (value: PollType) => void;
}){

    return(
    <div className="space-y-1.5">
        <Label>回答方式</Label>

        <div className="flex gap-2">
            {(["SINGLE", "MULTI"] as const).map((t) => (
                <Button
                    key={t}
                    type="button"
                    variant={value === t ? "default" : "outline"}
                    onClick={() => onChange(t)}
                    className="flex-1"
                >
                {t ==="SINGLE" ? "単一選択": "複数選択"}
                </Button>
            ))}
        </div>
    </div>
    )}