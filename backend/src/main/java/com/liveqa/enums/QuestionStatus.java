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
    ARCHIVED;

    /**
     * 遷移可否の判定を行うメソッド
     * OPEN→ANSWERED / OPEN→ARCHIVED / ANSWERED→ARCHIVED。ARCHIVED からは戻さない
     * @param next 遷移先
     * @return 遷移してよければtrue
     */
    public boolean canTransitionTo(QuestionStatus next) {
        return switch(this) {
            case OPEN -> next == ANSWERED || next == ARCHIVED;
            case ANSWERED -> next == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
