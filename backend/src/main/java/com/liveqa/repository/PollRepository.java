package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.liveqa.domain.Poll;
import com.liveqa.enums.PollStatus;

/**
 * pollsテーブルを操作するRepository
 * PollRepository
 * @author ichikura
 */
public interface PollRepository extends JpaRepository<Poll, Long> {

    /**
     * イベント内のPollを作成順で返すメソッド
     * 
     * @param eventId イベントID
     * @return ポール一覧
     */
    List<Poll> findByEvent_IdOrderByCreatedAtAsc(Long eventId);

    /**
     * イベント配下の指定ステータスのポールを作成順で返すメソッド
     * 参加者一覧はOPENを渡す
     * 
     * @param eventId イベントid
     * @param status  ポールのステータス
     * @return ポール一覧（古い順）
     */
    List<Poll> findByEvent_IdAndStatusOrderByCreatedAtAsc(Long eventId, PollStatus status);

    /**
     * ポールを主催者つきで復元するメソッド
     * 0件は無い／他人所有を区別しない
     * 
     * @param id     ポールid
     * @param hostId 主催者id
     * @return ポール1件
     */
    Optional<Poll> findByIdAndEvent_Host_Id(Long id, Long hostId);

}
