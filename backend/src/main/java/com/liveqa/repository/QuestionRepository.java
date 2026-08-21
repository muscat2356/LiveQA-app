package com.liveqa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * いいね数の加算を行うメソッド（原子的）
     * @param id 質問id
     * @return 更新行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Question q SET q.upvoteCount = q.upvoteCount + 1, q.updatedAt = CURRENT_TIMESTAMP WHERE q.id = :id")
    int incrementUpvoteCount(@Param("id") Long id);
    /**
     * いいね数の減算を行うメソッド（0未満にしない）
     * @param id 質問id
     * @return 更新行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Question q SET q.upvoteCount = q.upvoteCount - 1, q.updatedAt = CURRENT_TIMESTAMP WHERE q.id = :id AND q.upvoteCount > 0")
    int decrementUpvoteCount(@Param("id") Long id);

}
