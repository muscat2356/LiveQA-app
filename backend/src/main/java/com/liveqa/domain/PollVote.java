package com.liveqa.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * テーブルpoll_responsesに対応するEntity（選択肢×voter_keyのUNIQUE）
 * DTOのPollResponseとは別物
 * PollVote
 * @author ichikura
 */
@Entity
@Table(
    name = "poll_responses",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_poll_responses_option_voter",
        columnNames = { "poll_option_id", "voter_key" }
    )
)
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_option_id")
    private PollOption pollOption;

    @Column(name = "voter_key", nullable = false, length = 64)
    private String voterKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PollVote() {}

    /**
     * 回答1件のインスタンス化を行うファクトリ
     * 所属ポールはoptionから取る。個数・OPEN判定はService
     * @param pollOption 選んだ選択肢
     * @param voterKey g:{guestId}またはu:{userId}
     * @return インスタンス化したPollVote
     */
    public static PollVote cast(PollOption pollOption, String voterKey) {
        PollVote vote = new PollVote();
        vote.poll = pollOption.getPoll();
        vote.pollOption = pollOption;
        vote.voterKey = voterKey;
        vote.createdAt = Instant.now();
        return vote;
    }

    public Long getId() {
        return id;
    }

    public Poll getPoll() {
        return poll;
    }

    public PollOption getPollOption() {
        return pollOption;
    }

    public String getVoterKey() {
        return voterKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
