package com.liveqa.domain;

import java.time.Instant;

import com.liveqa.enums.EventStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Eventのドメイン
 * @author ichikura
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "join_code", nullable = false, unique = true, length = 12)
    private String joinCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();


    protected Event() {}

    /**
     * 新規作成時のインスタンス化ファクトリ
     * @param host User情報
     * @param title Eventタイトル
     * @param joinCode 参加コード、生成はService責務
     * @return インスタンス化したEvent（新規はDRAFT固定）
     */
    public static Event create(
        User host,
        String title,
        String joinCode
    ) {
        Event e = new Event();
        e.host = host;
        e.title = title;
        e.joinCode = joinCode;
        e.status = EventStatus.DRAFT;
        e.createdAt = Instant.now();
        e.updatedAt = Instant.now();

        return e;
    }

    public Long getId() {
        return id;
    }

    public User getHost() {
        return host;
    }

    public String getTitle() {
        return title;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    

}
