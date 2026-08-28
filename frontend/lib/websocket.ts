const LOCAL_FRONTEND_PORT = "3000";
const LOCAL_BACKEND_PORT = "8081";
const LOCAL_WEBSOCKET_URL = "ws://localhost:8081/ws";

/**
 * 実行環境に応じたWebSocket接続先を返す。
 */
export function getWebSocketBrokerUrl(): string {
    const configuredUrl = process.env.NEXT_PUBLIC_WEBSOCKET_URL;
    if (configuredUrl) {
        return configuredUrl;
    }

    if (typeof window === "undefined") {
        return LOCAL_WEBSOCKET_URL;
    }

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const host = window.location.port === LOCAL_FRONTEND_PORT
        ? `${window.location.hostname}:${LOCAL_BACKEND_PORT}`
        : window.location.host;

    return `${protocol}//${host}/ws`;
}
