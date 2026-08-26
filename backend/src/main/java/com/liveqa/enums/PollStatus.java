package com.liveqa.enums;

/**
 * ポール（polls.status）の状態（MVP-B）
 * 遷移はDRAFT→OPEN→CLOSED。CLOSEDは終端、DRAFTへは戻さない
 * PollStatus
 * @author ichikura
 */
public enum PollStatus {
    /** 作成直後・回答受付前 */
    DRAFT,
    /** 回答受付中 */
    OPEN,
    /** 締切（終端） */
    CLOSED;

    /**
     * 遷移可能可否の判定メソッド
     * DRAFT→OPEN、OPEN→CLOSED。CLOSEDは終端。DRAFTへは戻さない
     * @param next 遷移先
     * @return 遷移してOKならtrue
     */
    public boolean canTransitionTo(PollStatus next) {
        return switch (this) {
            case DRAFT -> next == PollStatus.OPEN;
            case OPEN -> next == PollStatus.CLOSED;
            case CLOSED -> false;
        };
    }
}
