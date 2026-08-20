package com.liveqa.enums;

/**
 * 質問（questions.status）の状態
 * 遷移はOPEN→ANSWERED→ARCHIVED（OPEN→ARCHIVEDも可）。ARCHIVEDからは戻さない
 * QuestionStatus
 * @author ichikura
 */
public enum QuestionStatus {
    /** 未回答 */
    OPEN,
    /** 回答済み */
    ANSWERED,
    /** アーカイブ */
    ARCHIVED
}
