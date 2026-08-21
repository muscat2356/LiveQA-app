"use server";

import { redirect } from "next/navigation";

export type JoinState = {
    error?: string
};

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

    const res = await fetch(
  `${process.env.API_BASE_URL}/api/join/${encodeURIComponent(joinCode)}`,
  { cache: "no-store", headers: { Accept: "application/json" } }
    );

    if(res.status === 404){
        return { error: "該当するルームがありません"};
    }

    if(!res.ok){
        return{ error: "参加コードが不正です"};
    }

     redirect(`/join/${joinCode}`);
}