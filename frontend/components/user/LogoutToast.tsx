"use client"

import { useEffect } from "react"
import { toast } from "sonner";

export function LogoutToast(){
    useEffect(() =>{
        if(sessionStorage.getItem("logout")){
            sessionStorage.removeItem("logout");
            toast.success("ログアウトしました");
        }
    }, []);

    return null;
}