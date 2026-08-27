package com.liveqa.service;

import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.dto.response.VoteResponse;

/**
 * いいね処理後のREST応答とWebSocket配信データ。
 *
 * @param response REST APIへ返すいいね結果
 * @param question WebSocketで配信する更新後の質問情報
 * @author mizumura
 */
public record VoteResult(
        VoteResponse response,
        QuestionResponse question
) {
}
