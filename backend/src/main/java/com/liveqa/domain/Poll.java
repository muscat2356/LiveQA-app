package com.liveqa.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * テーブルpollsに対応するEntity
 * Poll
 * @author ichikura
 */
@Entity
@Table(name = "polls")
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollType type = PollType.SINGLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status = PollStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PollOption> options = new ArrayList<>();

    protected Poll() {}

    /**
     * 新規作成時のインスタンス化ファクトリ
     * 個数・重複・桁数の検証はService。新規はDRAFT固定
     * @param event 所属イベント
     * @param questionText 設問文（trim済み）
     * @param type SINGLE / MULTI
     * @param labels 選択肢ラベル（trim済み、表示順はこのリスト順）
     * @return インスタンス化したPoll
     */
    public static Poll create(
            Event event,
            String questionText,
            PollType type,
            List<String> labels
    ) {

        if (labels.isEmpty()) {
            throw new IllegalArgumentException("選択肢が空です");
        }

        Poll poll = new Poll();
        poll.event = event;
        poll.questionText = questionText;
        poll.type = type;
        poll.status = PollStatus.DRAFT;
        poll.createdAt = Instant.now();

        int displayOrder = 0;
        for (String label : labels) {
            poll.options.add(PollOption.create(poll, label, displayOrder));
            displayOrder++;
        }
        return poll;
    }

    /**
     * ポールステータスの更新を行うメソッド
     * 遷移可否の検証はService
     * @param next 遷移先のステータス
     */
    public void changeStatus(PollStatus next) {
        this.status = next;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public String getQuestionText() {
        return questionText;
    }

    public PollType getType() {
        return type;
    }

    public PollStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PollOption> getOptions() {
        return options;
    }
}
