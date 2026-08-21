package com.liveqa.enums;

/**
 * 質問一覧の並び（クエリ sort）
 * QuestionSort
 * @author ichikura
 */
public enum QuestionSort {
    /** いいね多い順、同数なら新しい順 */
    POPULAR,
    /** 投稿の新しい順 */
    NEWEST;

    /**
     * クエリ文字列から並びを復元するメソッド
     * null / 空は popular。popular / newest 以外は IllegalArgumentException
     * @param raw クエリ sort
     * @return 並び
     */
    public static QuestionSort form(String raw) {
        if (raw == null || raw.isBlank()) {
            return POPULAR;
        }
        if ("popular".equals(raw)) {
            return POPULAR;
        }
        if ("newest".equals(raw)) {
            return NEWEST;
        }
        throw new IllegalArgumentException("sortは popular / newest のみです。");
    }

}
