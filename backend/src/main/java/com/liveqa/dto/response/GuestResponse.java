package com.liveqa.dto.response;

import com.liveqa.domain.VoterKey;

/**
 * 匿名セッション発行POSTの応答DTO
 * voterKeyはg:{guestId}
 * GuestResponse
 * @author ichikura
 */
public record GuestResponse(
    String guestId,
    String voterKey
) {

    /**
     * guestIdから応答DTOへ変換するファクトリ
     * voterKeyは値オブジェクトVoterKeyが組み立てる
     * @param guestId 発行または再利用したguestId
     * @return 応答DTO
     */
    public static GuestResponse form(String guestId) {
        return new GuestResponse(guestId, VoterKey.guest(guestId).value());
    }
}
