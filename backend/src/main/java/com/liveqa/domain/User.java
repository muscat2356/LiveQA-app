package com.liveqa.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * テーブルusersに対応するEntity（HOSTのJIT用）
 * User
 * @author ichikura
 */

@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_users_oidc_subject",
        columnNames = "oidc_subject"
    )
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oidc_subject", nullable = false)
    private String oidcSubject;

    @Column(nullable = false, length = 20)
    private String role = "HOST";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    /**
     * 専用のファクトリ、インスタンス化の入り口はここだけ
     * @param oidcSubject Keycloakのsub
     * @return HOSTとして生成したUser
     */
    public static User hostOf(String oidcSubject) {
        User user = new User();

        user.oidcSubject = oidcSubject;
        user.role = "HOST";
        user.createdAt = Instant.now();

        return user;
    }

    public Long getId() {
        return id;
    }

    public String getOidcSubject() {
        return oidcSubject;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    

}
