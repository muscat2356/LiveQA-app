package com.liveqa.websocket;

import java.util.List;

import com.liveqa.dto.response.PollOptionResponse;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.enums.PollStatus;

/**
 * WebSocketで配信するポール集計データ。
 * 回答者固有のmyOptionIdsは含めない。
 *
 * @param id ポールID
 * @param status ポールの状態
 * @param options 選択肢ごとの最新得票
 * @param totalResponses 全選択肢の得票合計
 * @author mizumura
 */
public record PollEventPayload(
        Long id,
        PollStatus status,
        List<PollOptionResponse> options,
        int totalResponses
) {

    /**
     * REST APIのポール応答データからWebSocket配信用データを生成する。
     *
     * @param response 変換元のポール応答データ
     * @return WebSocket配信用のポール集計データ
     */
    public static PollEventPayload from(PollResponse response) {
        return new PollEventPayload(
                response.id(),
                response.status(),
                List.copyOf(response.options()),
                response.totalResponses());
    }
}
