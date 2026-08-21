package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * 質問を主催者つきで復元するメソッド
     * 0件は他イベント又は存在しない
     * @param id 質問id
     * @param hostId 主催者id
     * @return 質問1件
     */
    Optional<Question> findByIdAndEvent_Host_Id(Long id, Long hostId);
}
