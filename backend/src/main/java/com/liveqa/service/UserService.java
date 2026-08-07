package com.liveqa.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liveqa.domain.User;
import com.liveqa.dto.response.MeResponse;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.UserRepository;

/**
 * usersテーブルとIDPの橋渡しを行うサービス
 * ログイン成功時等に、oidcSubjectでusersを索引
 * @author ichikura
 * 
 */
@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
    
    /**
     * ログイン成功時にoidcSubjectでusersの存在確認をするメソッド
     * 存在がなければ新規でusersに情報をINSERT
     * @param oidcSubject ログイン成功時にJWTから取得する
     * @return ユーザー情報
     */
    @Transactional
    public User provisionHost(String oidcSubject) {
        return repository.findByOidcSubject(oidcSubject)
                .orElseGet(() -> repository.save(User.hostOf(oidcSubject)));
    }

    /**
     * セッションから復元されたOidcUserのoidcSubjectで存在確認をするメソッド
     * @param oidcSubject セッションから復元されたuserが保持しているsubjectt
     * @return 検索されたユーザー情報
     */
    @Transactional
    public User requireByOidcSubject(String oidcSubject) {
        return repository.findByOidcSubject(oidcSubject)
                .orElseThrow(() -> new BusinessException(
                    HttpStatus.NOT_FOUND, "Not Found", "ユーザーが見つかりません。"
                ));
    }

    /**
     * ローカルUser + OIDCクレームからResponseを組み立てる
     * @param local DBのローカルユーザー
     * @param oidcUser OIDCクレーム情報
     * @return MeResponse
     */
    public MeResponse toMeResponse(User local, OidcUser oidcUser) {
        final String displayName = oidcUser.getFullName() != null
                ? oidcUser.getFullName()
                : oidcUser.getPreferredUsername();
        return new MeResponse(
            local.getId(),
            displayName,
            oidcUser.getEmail(),
            local.getRole()
        );
    }

    /**
     * DB空の復元 + 組み立てを呼び出すメソッド
     * @param oidcUser OIDCユーザー情報
     * @return レスポンス
     */
    @Transactional
    public MeResponse currentHost(OidcUser oidcUser) {
        final User local = requireByOidcSubject(oidcUser.getSubject());
        return toMeResponse(local, oidcUser);
    }
}
