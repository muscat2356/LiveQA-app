package com.liveqa.websocket;

/**
 * 質問に関するWebSocket配信イベントの種類。
 * 
 * @author mizumura
 */
public enum QuestionEventType {

    /** 質問が作成された */
    QUESTION_CREATED,

    /** いいね数が変化した */
    QUESTION_UPVOTED,

    /** 質問の状態が変化した */
    QUESTION_ANSWERED

}
