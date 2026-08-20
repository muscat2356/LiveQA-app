package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Vote;

/**
 * votesテーブルを操作するRepository
 * VoteRepository
 * @author ichikura
 */
public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * あるイベント内でそのvoter_keyが付けたいいね一覧を返すメソッド
     * 一覧のvotedByMe判定に使う
     * @param eventId イベントid
     * @param voterKey g:{guestId}またはu:{userId}
     * @return いいね一覧
     */
    List<Vote> findByQuestion_Event_IdAndVoterKey(Long eventId, String voterKey);

    /**
     * 同じ質問への二重いいね有無を返すメソッド
     * @param questionId 質問id
     * @param voterKey g:{guestId}またはu:{userId}
     * @return 既にあればtrue
     */
    boolean existsByQuestion_IdAndVoterKey(Long questionId, String voterKey);

    /**
     * 質問といいねした人の組で1件を検索するメソッド
     * @param questionId 質問id
     * @param voterKey g:{guestId}またはu:{userId}
     * @return Optional（無い可能性があるため）
     */
    Optional<Vote> findByQuestion_IdAndVoterKey(Long questionId, String voterKey);

    /**
     * 質問といいねした人の組でいいねを削除するメソッド
     * @param questionId 質問id
     * @param voterKey g:{guestId}またはu:{userId}
     */
    void deleteByQuestion_IdAndVoterKey(Long questionId, String voterKey);
}
