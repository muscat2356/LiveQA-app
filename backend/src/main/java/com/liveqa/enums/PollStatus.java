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
    CLOSED
}
