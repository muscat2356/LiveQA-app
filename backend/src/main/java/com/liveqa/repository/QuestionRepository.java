package com.liveqa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Question;

/**
 * questionsテーブルを操作するRepository
 * QuestionRepository
 * @author ichikura
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * イベント配下の質問一覧を返すメソッド
     * 新しい順（created_at DESC）
     * @param eventId イベントid
     * @return 質問一覧
     */
    List<Question> findByEvent_IdOrderByCreatedAtDesc(Long eventId);
}
