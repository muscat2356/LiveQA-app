package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.liveqa.domain.Event;
import com.liveqa.enums.EventStatus;

/**
 * Eventsテーブルを操作するRepository
 * EventRepository
 * @author ichikura
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 参加コードの検証をするメソッド
     * @param joinCode 入力された参加コード
     * @return コードとイベントが一致したときtrue
     */
    boolean existsByJoinCode(String joinCode);

    /**
     * ログイン中の主催者のイベント一覧を返すメソッド
     * DELETEDは載せない
     * @param hostId ログイン中の主催者のid
     * @param status 除外するステータス（DELETED）
     * @return イベント一覧
     */
    List<Event> findByHost_IdAndStatusNotOrderByCreatedAtDesc(Long hostId, EventStatus status);

    /**
     * 参加コードでイベントを検索するメソッド
     * DELETEDはempty
     * @param joinCode 参加コード
     * @param status 除外するステータス（DELETED）
     * @return Optional（無い可能性があるため）
     */
    Optional<Event> findByJoinCodeAndStatusNot(String joinCode, EventStatus status);

    /**
     * ログイン中の主催者が選択した特定のイベント情報を返すメソッド
     * DELETEDはempty
     * @param id イベントid
     * @param hostId ログイン中の主催者id
     * @param status 除外するステータス（DELETED）
     * @return Optional（無い可能性があるため）
     */
    Optional<Event> findByIdAndHost_IdAndStatusNot(Long id, Long hostId, EventStatus status);

    /**
     * idでイベントを検索するメソッド
     * DELETEDはempty（質問API用）
     * @param id イベントid
     * @param status 除外するステータス（DELETED）
     * @return Optional（無い可能性があるため）
     */
    Optional<Event> findByIdAndStatusNot(Long id, EventStatus status);

    /**
     * 質問数の加算を行うメソッド（原子的）
     * versionは触らない（名称・削除の楽観ロックと混ぜない）
     * @param id イベントid
     * @return 更新行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Event e SET e.questionCount = e.questionCount + 1, e.updatedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    int incrementQuestionCount(@Param("id") Long id);
}
