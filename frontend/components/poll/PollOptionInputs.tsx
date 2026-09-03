"use client"

import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { OptionInput } from "../../features/poll/usePollForm"

export default function PollOptionInputs({
    options,
    onAdd,
    onRemove,
    onEdit,
    hasDuplicate,
}:{
    options: OptionInput[];
    onAdd: () => void;
    onRemove: (key: number) => void;
    onEdit: (key: number, label: string) => void;
    hasDuplicate: boolean;
}){

    return(
        <div className="space-y-2">
            <Label>選択肢(2~10個)</Label>

            {options.map((o, i) => (
                <div key={o.key} className="flex items-center gap-2">
                    <Input
                        value={o.label}
                        onChange={(e) => onEdit(o.key, e.target.value)}
                        placeholder={`選択肢${i + 1}`}
                        maxLength={100}
                        className="flex-1"
                    />
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => onRemove(o.key)}
                        disabled={options.length <= 2}
                        className="shrink-0 text-muted-foreground"
                    >
                        削除
                    </Button>
                </div>
            ))}

            {options.length < 10 &&
                <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={onAdd}
                className="w-full"
                >
                    追加
                </Button>
            }

            {hasDuplicate&&(
                <p className="text-sm text-destructive">選択肢が重複しています</p>
            )}
        </div>
    )
}