package com.liveqa.enums;

import java.util.Optional;

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

    /**
     * クエリ文字列からstatusを復元するメソッド
     * null / 空はempty（呼び出し側がARCHIVED以外にする）
     * OPEN / ANSWERED / ARCHIVED 以外は IllegalArgumentException
     * @param raw クエリ status
     * @return 指定があればその値、未指定はempty
     */
    public static Optional<QuestionStatus> form(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(QuestionStatus.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "statusは OPEN / ANSWERED / ARCHIVED のみです。");
        }
    }
}
