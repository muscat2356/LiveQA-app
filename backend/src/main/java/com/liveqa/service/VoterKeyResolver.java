package com.liveqa.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.liveqa.domain.User;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.exception.BusinessException;

/**
 * VoterKeySourceからVoterKeyへ解決するサービスクラス
 * VoterKeyResolver
 * @author ichikura
 */
@Service
public class VoterKeyResolver {

    private final UserService userService;

    public VoterKeyResolver(UserService userService) {
        this.userService = userService;
    }

    /**
     * 投票キーの解決を試みるメソッド
     * Missingはempty（一覧のvotedByMeは全部false）
     * Hostはusers検索、無ければ404
     * @param source 投票キーの取得元
     * @return あればVoterKey、無ければempty
     */
    public Optional<VoterKey> tryResolve(VoterKeySource source) {
        return switch (source) {
            case VoterKeySource.Host(String subject) -> {
                final User user = userService.requireByOidcSubject(subject);
                yield Optional.of(VoterKey.host(user.getId()));
            }
            case VoterKeySource.Guest(String guestId) -> Optional.of(VoterKey.guest(guestId));
            case VoterKeySource.Missing() -> Optional.empty();
        };
    }

    /**
     * 投票キーの解決を行うメソッド
     * 未確立は401（いいね用）
     * @param source 投票キーの取得元
     * @return 解決したVoterKey
     */
    public VoterKey resolve(VoterKeySource source){
        return tryResolve(source).orElseThrow(this::unauthorized);
    }

    /**
     * 識別が確立していることを検証するメソッド
     * 未確立は401（質問POST用）。キー自体は使わない
     * @param source 投票キーの取得元
     */
    public void require(VoterKeySource source) {
        resolve(source);
    }

    /**
     * 匿名セッション未確立の401を組み立てるメソッド
     * @return UnauthorizedのBusinessException
     */
    private BusinessException unauthorized () {
        return new BusinessException(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "匿名セッションが未確立です。"
        );
    }

}
