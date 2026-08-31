"use server";

import { fetchJoinEvent } from "@/lib/api/server/eventApi";
import { redirect } from "next/navigation";

export type JoinState = {
    error?: string
};

/**
 * 確認コードの入力確認
 * @param _prev 
 * @param formData 
 * @returns 
 */
export async function verifyJoinCode(
    _prev: JoinState,
    formData: FormData
): Promise<JoinState> {

    const joinCode = String(formData.get("joinCode") ?? "").trim();
    if(!joinCode){
        return{
            error: "参加コードを入力してください。"
        };
    }
    try{
        const event = await fetchJoinEvent(joinCode);

        if(!event){
            return{error: "該当するルームがありません"}
        }
    }catch{
        return{error: "参加コードが不正です"}
    }

     redirect(`/join/${joinCode}`);
}