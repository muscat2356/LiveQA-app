const LOCAL_WEBSOCKET_URL = process.env.NEXT_PUBLIC_WEBSOCKET_URL;

function websocketUrlFrom(baseUrl: string): string {
    const websocketUrl = new URL("/ws", baseUrl);
    websocketUrl.protocol = websocketUrl.protocol === "https:" ? "wss:" : "ws:";
    return websocketUrl.toString();
}

/**
 * 実行環境に応じたWebSocket接続先を返す。
 */
export function getWebSocketBrokerUrl(): string {
    const configuredUrl = process.env.NEXT_PUBLIC_WEBSOCKET_URL;
    if (configuredUrl) {
        return configuredUrl;
    }

    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
    if (apiBaseUrl) {
        return websocketUrlFrom(apiBaseUrl);
    }

    if (typeof window === "undefined") {
        return LOCAL_WEBSOCKET_URL;
    }

    return websocketUrlFrom(window.location.origin);
}
