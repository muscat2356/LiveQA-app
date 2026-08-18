'use client'

import { useActionState } from "react"
import { verifyJoinCode } from "./actions";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export default function JoinForm(){
    const[state, formAction, pending] = useActionState(verifyJoinCode, {});

    
    return(
        <form action={formAction}>
            <Input
                name="joinCode"
                placeholder="ABCD1234"
                autoComplete="off"
                autoCapitalize="none"
                required
            />
            {state.error && (
                <p role="alert">{state.error}</p>
            )}
            <Button type="submit" disabled={pending}>
                {pending ? "確認中..." : "参加する"}
            </Button>
        </form>
    );
}