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

@Entity
@Table(
    name = "votes",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_votes_question_voter",
        columnNames = { "question_id", "voter_key" }
    )
)
public class Vote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "voter_key", nullable = false, length = 64)
    private String voterKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Vote() {}

    public static Vote cast(Question question, String voterKey) {
        Vote v = new Vote();
        v.question = question;
        v.voterKey = voterKey;
        v.createdAt = Instant.now();
        return v;
    }

    public Long getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public String getVoterKey() {
        return voterKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    
}
