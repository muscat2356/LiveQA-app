"use client"

import { readCookie } from "@/components/cookie";
import { Button } from "@/components/ui/button";
import { useState } from "react";
import { toast } from "sonner";

export default async function LogoutButton({ 
    id 
}:{ 
    id : string
}){

    const[ pending, setPending ] = useState(false);
    const[ error, setError ] = useState("");


    async function handleSubmit(){

        setPending(true);

        try{
            const xsrfToken = readCookie("XSRF-TOKEN");
            const res = await fetch(`/api/auth/logout`,{
                method: "POST",
                headers: {
                    ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken } : {}),
                },
                credentials: "same-origin",
            });

            if(res.ok){
                const problem = await res.json().catch(() => null);
                throw new Error(problem?.message ?? "エラーが発生しました");

            }
        }catch(e){
            toast.error("ログアウトに失敗しました");
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        }finally{
            setPending(false);
        }
    }

    return(
        <div>
            <Button
                onClick={handleSubmit}
                disabled={pending}
            >
                {pending ? "ログアウト中" : "ログアウト"}
            </Button>
        </div>
    );
}