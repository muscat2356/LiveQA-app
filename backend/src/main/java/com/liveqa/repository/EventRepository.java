package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Event;

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
     * @param hostId ログイン中の主催者のid
     * @return イベント一覧
     */
    List<Event> findByHost_IdOrderByCreatedAtDesc(Long hostId);

    /**
     * 参加コードでイベントを検索するメソッド
     * @param joinCode 参加コード
     * @return Optional（無い可能性があるため）
     */
    Optional<Event> findByJoinCode(String joinCode);

    /**
     * ログイン中の主催者が選択した特定のイベント情報を返すメソッド
     * @param id イベントid
     * @param hostId ログイン中の主催者id
     * @return Optional（無い可能性があるため）
     */
    Optional<Event> findByIdAndHost_Id(Long id, Long hostId);
}
