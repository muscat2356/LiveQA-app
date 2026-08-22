"use client"

import { readCookie } from "@/components/cookie";
import { Button } from "@/components/ui/button";
import { useEffect, useState } from "react";
import { toast } from "sonner";

export default function LogoutButton(){

    const [ xsrfToken, setXsrfToken ] = useState("");

    useEffect(() => {
        setXsrfToken(readCookie("XSRF-TOKEN") ?? "");
    }, []);

    return(
        <form method="POST" action="/api/auth/logout">
            <input type="hidden" name="_csrf" value={xsrfToken} />
            <Button type="submit" variant="outline" size="lg" onClick={() => sessionStorage.setItem("logout", "1")}>
                ログアウト
            </Button>
        </form>
    );
}