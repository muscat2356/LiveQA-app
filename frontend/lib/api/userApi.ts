export async function fetchPostUserRegister( email: string ): Promise<void>{

    const res = await fetch (` /api/auth/register`,{
        method:"POST",
        headers:{
            "Content-Type":"application/json",
        },
        credentials: "same-origin",
    });

    if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? "エラーが発生しました");
    }

}