"use client"

import { fetchPostUserRegister } from "@/lib/api/userApi"
import { useCallback, useRef, useState } from "react";

export default function useUserRegister(){

    const [ email, setEmail ] = useState(""); 
    const [ pending, setPending ] = useState(false);
    const [ error, setError ] = useState("");
    const [ done, setDone ] = useState(false);
    const inFlight = useRef(false);

    const register = useCallback(async () => {
        
        if(inFlight.current) return;
        inFlight.current = true;

        try{
            setPending(true);
            setError("");

            await fetchPostUserRegister(email);

            setDone(true);

        }catch(e){
            setError(e instanceof Error ? e.message : "エラーが発生しました");
        }finally{
            setPending(false);
            inFlight.current = false;
        }

    }, [email]);


    return{ email, setEmail, pending, error, done, register}



}