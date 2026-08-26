"use client"

import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { OptionInput } from "./usePollForm"

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
        <div>
            <Label>選択肢(2~10個)</Label>

            
            {options.map((o, i) => (
                <div key={o.key}>
                    <Input
                        value={o.label}
                        onChange={(e) => onEdit(o.key, e.target.value)}
                        placeholder={`選択肢${i + 1}`}
                        maxLength={100}
                    />
                    <Button
                        type="button"
                        variant="ghost"
                        onClick={() => onRemove(o.key)}
                        disabled={options.length <= 2}
                    >
                        削除
                    </Button>
                </div>
            ))}

            {options.length < 10 &&
                <Button
                type="button"
                onClick={onAdd}
                >   
                    追加
                </Button>
            }

            {hasDuplicate === true &&(
                
            )
            
            }
        </div>
    )
}