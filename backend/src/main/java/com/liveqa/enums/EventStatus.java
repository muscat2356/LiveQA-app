package com.liveqa.enums;

/**
 * ルーム（events.status）の状態
 * EventStatus
 * @author ichikura
 */
public enum EventStatus {
    /** 作成直後・受付前 */
    DRAFT,
    /** 質問・いいね受付中 */
    OPEN,
    /** 締切・閲覧のみ。OPENへ再開可 */
    CLOSED,
    /** 論理削除 */
    DELETED;

    /**
     * 遷移可否の判定を行うメソッド
     * DRAFT→OPEN、OPEN⇔CLOSED。DRAFTへは戻さない
     * DELETEDは削除のため、状態遷移不可
     * @param next 遷移先
     * @return 遷移してよければtrue
     */
    public boolean canTransitionTo(EventStatus next) {
        return switch (this) {
            case DRAFT -> next == OPEN;
            case OPEN -> next == CLOSED;
            case CLOSED -> next == OPEN;
            case DELETED -> false;
        };
    }
}
