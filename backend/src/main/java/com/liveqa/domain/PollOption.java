package com.liveqa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * テーブルpoll_optionsに対応するEntity
 * PollOption
 * @author ichikura
 */
@Entity
@Table(name = "poll_options")
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    protected PollOption() {}

    /**
     * 選択肢1件のインスタンス化を行うファクトリ
     * 個数・重複・桁数の検証はService
     * @param poll 所属ポール
     * @param label ラベル（trim済み）
     * @param displayOrder 表示順（0始まり）
     * @return インスタンス化したPollOption
     */
    public static PollOption create(Poll poll, String label, int displayOrder) {
        PollOption option = new PollOption();
        option.poll = poll;
        option.label = label;
        option.displayOrder = displayOrder;
        return option;
    }

    public Long getId() {
        return id;
    }

    public Poll getPoll() {
        return poll;
    }

    public String getLabel() {
        return label;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
