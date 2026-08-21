import { readCookie } from "@/components/cookie";

let guestId: Promise<void> | null = null;

export function getGuestSession(): Promise<void> {
    if(!guestId){
        guestId = (async () =>{
            const xsrfToken = readCookie("XSRF-TOKEN");
            const res = await fetch("/api/auth/guest", {
                method: "POST",
                headers: { ...(xsrfToken ? { "X-XSRF-TOKEN": xsrfToken }: {}) },
                credentials: "same-origin"
            });

            if(!res.ok) throw new Error(`ゲストセッションの発行に失敗しました: ${res.status}`);
        })();
    }
    return guestId;
}