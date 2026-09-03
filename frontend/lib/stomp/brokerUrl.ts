/**
 * STOMP接続先の解決
 *
 * 環境変数が入っている場合に、環境変数の値から組み立て
 * 未設定の場合に現在のオリジンから設定
 * HTTPSの場合に、wssになるため、設定
 * @returns
 */
export function resolveBrokerUrl(): string {

    const configured = process.env.NEXT_PUBLIC_WS_URL;

    if(configured){
        return configured;
    }

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    return `${protocol}//${window.location.host}/ws`;
}
