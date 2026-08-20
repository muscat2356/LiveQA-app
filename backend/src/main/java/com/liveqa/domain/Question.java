package com.liveqa.domain;

import java.time.Instant;

import com.liveqa.enums.QuestionStatus;

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
 * テーブルquestionsに対応するEntity
 * Question
 * @author ichikura
 */
@Entity
@Table(name = "questions")
public class Question {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status = QuestionStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Question() {}

    /**
     * 質問投稿時のインスタンス化を行うファクトリ
     * 空ニックネームを「匿名」にする処理とquestion_count加算はService
     * @param event 所属イベント
     * @param body 質問本文
     * @param displayName 表示名（Service側で空なら「匿名」済み）
     * @return インスタンス化したQuestion（statusはOPEN、upvoteCountは0）
     */
    public static Question post(
        Event event,
        String body,
        String displayName    
    ) {
        Question q = new Question();
        q.event = event;
        q.body = body;
        q.displayName = displayName;
        q.upvoteCount = 0;
        q.status = QuestionStatus.OPEN;
        q.createdAt = Instant.now();
        q.updatedAt = Instant.now();
        return q;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getBody() {
        return body;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getUpvoteCount() {
        return upvoteCount;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    

}
