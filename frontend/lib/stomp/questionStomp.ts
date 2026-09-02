import { QuestionEvent} from "@/types/question/question";
import { Client } from "@stomp/stompjs";

/**
 * 購読解除関数
 */
export type Unsubscribe = () => void;

/**
 * 接続先の解決
 * 
 * 環境変数が入っている場合に、環境変数の値から組み立て
 * 未設定の場合に現在のオリジンから設定
 * HTTPSの場合に、wssになるため、設定
 * @returns 
 */
function resolveBrokerUrl(): string {

    const configured = process.env.NEXT_PUBLIC_WS_URL;

    if(configured){
        return configured;
    }

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";

    return `${protocol}//${window.location.host}/ws`;
}
/**
 * 指定イベントの質問トピックの購読登録処理
 * 呼び出しの共通化
 * 
 * 並び替えやフィルタが変わっても再接続しないように
 * 受信後の処理はコールバックとして外から渡す
 * 
 * @param eventId 
 * @param onEvent 
 * @returns 
 */
export function subscribeQuestions(
    eventId: number,
    onEvent: (event: QuestionEvent) => void,
): Unsubscribe {
    const client = new Client({
        brokerURL: resolveBrokerUrl(),
        reconnectDelay: 5000,

        onConnect: () => {
            client.subscribe(
                `/topic/events/${eventId}/questions`,
                (message) => {
                    try{
                        const questionEvent: QuestionEvent = JSON.parse(message.body);
                        onEvent(questionEvent);
                    }catch(e){
                        //壊れたフレームで購読全体を止めないようにエラーを握りつぶす
                        console.error("STOMP メッセージの解析に失敗しました", e);
                    }
                }
            );
        },

        onStompError: (frame) => {
            console.error("STOMP エラー", frame.headers["message"]);
        },
    });

    client.activate();

    return () => {
        client.deactivate();
    }
}