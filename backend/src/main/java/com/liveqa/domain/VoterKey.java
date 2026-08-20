package com.liveqa.domain;

/**
 * 投票者識別子 g:{guestId} / u:{userId} の値オブジェクト
 * VoterKey
 * @author ichikura
 */
public record VoterKey(String value) {

    /**
     * 匿名用キーの発行を行うメソッド
     * @param guestId Session属性のguestId
     * @return g:{guestId}
     */
    public static VoterKey guest(String guestId) {
        return new VoterKey("g:" + guestId);
    }

    /**
     * HOST用キーの発行を行うメソッド
     * @param id ローカルusers.id（oidc_subjectではない）
     * @return u:{id}
     */
    public static VoterKey host(Long id) {
        return new VoterKey("u:" + id);
    }
}
