package com.liveqa.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.response.GuestResponse;
import com.liveqa.exception.BusinessException;

/**
 * 匿名セッションに対する業務処理を行うサービスクラス
 * GuestSessionService
 * @author ichikura
 */
@Service
public class GuestSessionService {

    /**
     * 匿名guestIdの発行または再利用を行うメソッド
     * Guestなら再利用、MissingならUUIDを新規発行
     * Hostは400（投票用のformを繋いではいけない）
     * voterKeyはg:{guestId}。Sessionへの書き込みはController
     * @param source Guest / Missing（Hostは不可）
     * @return guestIdとvoterKeyのResponseクラス
     */
    public GuestResponse issueOrReuse(VoterKeySource source) {
        return switch (source) {
            case VoterKeySource.Guest(String guestId) -> toGuestResponse(guestId);
            case VoterKeySource.Missing() -> toGuestResponse(UUID.randomUUID().toString());
            case VoterKeySource.Host(String ignored) -> throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Bad Request",
                    "匿名セッションの発行対象ではありません。");
        };
    }

    /**
     * レスポンスDTOへ変換するメソッド
     * @param guestId 発行または再利用したguestId
     * @return レスポンス
     */
    private GuestResponse toGuestResponse(String guestId) {
        return new GuestResponse(guestId, VoterKey.guest(guestId).value());
    }
}
