package com.liveqa.websocket;

/**
 * ポールに関するWebSocket配信イベントの種類。
 *
 * @author mizumura
 */
public enum PollEventType {

    /** ポールの回答受付が開始された */
    POLL_OPENED,

    /** ポールの集計結果が更新された */
    POLL_UPDATED,

    /** ポールの回答受付が終了した */
    POLL_CLOSED

}
