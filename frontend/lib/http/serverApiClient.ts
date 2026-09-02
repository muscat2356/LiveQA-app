import { cookies } from "next/headers";
import "server-only";
import { ApiError } from "./apiError";


/**
 * サーバ側でのみ実行ファイル
 * server-only
 */

/**
 * エラーレスポンス
 */
type ProblemBody = {
    message?: string;
    detail?: string;
}

const DEFAULT_ERROR_MESSAGE = "エラーが発生しました";

/**
 * ServerActions/ServerComponent用のAPIクライアント
 * 
 * GET専用　更新系はCSRFトークンが必要なため
 * @param path 
 * @returns 
 */
export async function serverApiClient<T>(path: string):Promise<T | null>{

    const baseUrl = process.env.API_BASE_URL;

    const cookieStore = await cookies();

    const res = await fetch(`${baseUrl}${path}`,{
        headers:{
            Accept: "application/json",
            Cookie: cookieStore.toString(),
        },
        cache: "no-store"
    });

    if(!res.ok){
        const problem: ProblemBody | null = await res.json().catch(() => null);

        const message = 
                problem?.message
                ?? problem?.detail
                ?? DEFAULT_ERROR_MESSAGE;
        
        throw new ApiError(res.status, message);
    }

    return res.json();
}


