import { apiClient } from "@/lib/http/apiClient";
import { readCookie } from "@/lib/http/cookie";

//発行したゲストセッション　重複を防ぐためにキャッシュ
let guestSession: Promise<void> | null = null;

/** 
 * 匿名セッションを発行
 * （同時に複数呼ばれても1回だけ発行） 
 * */
export function getGuestSession(): Promise<void> {
    if(!guestSession){

        guestSession = apiClient<void>('/api/auth/guest',{
            method:"POST",
        }).catch((e) => {
            //失敗した場合のキャッシュを再度初期に更新
            guestSession = null;
            throw e;
        });
    }
    return guestSession;
}