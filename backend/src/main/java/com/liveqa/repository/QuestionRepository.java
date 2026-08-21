package com.liveqa.repository;

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
public interface QuestionRepository extends JpaRepository<Question, Long>, QuestionQuery {

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
