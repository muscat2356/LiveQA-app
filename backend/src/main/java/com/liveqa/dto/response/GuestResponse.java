package com.liveqa.dto.response;

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

}
