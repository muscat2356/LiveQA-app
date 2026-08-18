"use client"

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { enterRoom, type EnterState } from "./actions";
import { useActionState} from "react"

export default function EnterForm({
        joinCode, 
        initialNickname,
    }:{
        joinCode: string; 
        initialNickname: string;
    }) {
    const [state, formAction, pending] = useActionState<EnterState, FormData>(
        enterRoom, 
        {}
    );

    return(
        <form action={formAction} className="space-y-3">
            <input type="hidden" name="joinCode" value={joinCode} />

            <div className="space-y-1.5">
                <Label htmlFor="nickname">ニックネーム（任意）</Label>
                <Input
                    id="nickname"
                    name="nickname"
                    placeholder="ニックネームを入力してください"
                    maxLength={20}
                    defaultValue={initialNickname}
                    autoComplete="off"
                />
                <p className="text-xs text-muted-foreground">
                    未入力の場合は「匿名」で表示されます
                </p>
            </div>

            {state.error && (
                <p className="text-sm text-red-600" role="alert">
                    {state.error}
                </p>
            )}

            <Button type="submit" disabled={pending} className="w-full">
                {pending ? "参加中...": "参加する"}
            </Button>
        </form>
    );
}