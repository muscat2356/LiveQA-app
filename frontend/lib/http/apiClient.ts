import { ApiError } from "./apiError";
import { readCookie } from "./cookie";

/**
 * HTTPメソッド定義
 */
type HttpMethod = "GET" | "POST" | "PATCH" | "DELETE";

/**
 * APIリクエスト用の型定義
 */
type ApiRequest = {
    method?:HttpMethod;
    body?: unknown;
    errorMessage?: Partial<Record<number, string>>;
}

/**
 * バックエンドのエラーレスポンス
 */
type ProblemBody = {
    message?: string;
    detail?: string;
}

const DEFAULT_ERROR_MESSAGE = "エラーが発生しました";

/**
 * ブラウザ専用APIの共通クライアント
 * XSRF　ヘッダー付与　credentials エラー整形
 * サーバからはdocumentを触れるため、触れない
 */
export async function apiClient<T>(
    path: string,
    request: ApiRequest = {}
): Promise<T> {

    const method: HttpMethod = request.method ?? "GET";
    const hasBody = request.body !== undefined;

    const xsrfToken = method === "GET"
            ? undefined
            : readCookie("XSRF-TOKEN");
    
    const res = await fetch(path, {
        method,
        headers:{
            Accept: "application/json",
            ...(hasBody ? { "Content-Type": "application/json" }: {}),
            ...(xsrfToken ? {"X-XSRF-TOKEN" : xsrfToken}: {}),
        },
        credentials: "same-origin",
        cache: "no-store",
        ...(hasBody ? { body: JSON.stringify(request.body) } : {}),
    });

    if(!res.ok){
        const problem: ProblemBody | null = await res.json().catch(() => null);

        const message =
            request.errorMessage?.[res.status]
            ?? problem?.message
            ?? problem?.detail
            ?? DEFAULT_ERROR_MESSAGE;

        throw new ApiError(res.status, message);
    }

    //204の場合に値を変換しないため
    if(res.status === 204){
        return undefined as unknown as T;
    }

    return res.json() as Promise<T>;
}
