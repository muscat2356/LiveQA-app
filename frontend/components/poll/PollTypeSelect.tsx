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
    <div>
        <Label>回答方式</Label>

            {(["SINGLE", "MULTI"] as const).map((t) => (
                <Button
                    key={t}
                    type="button"
                    variant={value === t ? "default" : "outline"}
                    onClick={() => onChange(t)}
                >
                {t ==="SINGLE" ? "単一選択": "複数選択"}
                </Button>
            ))}
    </div>
    )}