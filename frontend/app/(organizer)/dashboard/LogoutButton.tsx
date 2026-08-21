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
        <form action="">
            <input type="hidden" name="csrf" value={xsrfToken} />
            <Button type="submit" onClick={() => sessionStorage.setItem("logout", "1")}>
                ログアウト
            </Button>
        </form>
    );
}