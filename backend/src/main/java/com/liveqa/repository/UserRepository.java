package com.liveqa.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.User;

/**
 * usersテーブルを操作するRepository
 * UserRepository
 * @author ichikura
 */
public interface UserRepository extends JpaRepository<User, Long>{

    /**
     * oidc_subjectでHOST行を検索するメソッド
     * @param oidcSubject Keycloakのsub
     * @return Optional（無い可能性があるため）
     */
    Optional<User> findByOidcSubject(String oidcSubject);
}
