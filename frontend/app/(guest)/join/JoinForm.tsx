'use client'

import { useActionState } from "react"
import { verifyJoinCode } from "./actions";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export default function JoinForm(){
    const[state, formAction, pending] = useActionState(verifyJoinCode, {});

    
    return(
        <form action={formAction} className="space-y-4">
            <Input
                name="joinCode"
                placeholder="ABCD1234"
                autoComplete="off"
                autoCapitalize="none"
                required
                className="h-12 text-center text-lg font-mono tracking-[0.3em]"
            />
            {state.error && (
                <p role="alert" className="text-sm text-destructive">{state.error}</p>
            )}
            <Button type="submit" disabled={pending} size="lg" className="h-12 w-full text-base">
                {pending ? "確認中..." : "参加する"}
            </Button>
        </form>
    );
}