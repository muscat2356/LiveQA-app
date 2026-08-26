package com.liveqa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.liveqa.domain.PollVote;
import com.liveqa.repository.projection.OptionVoteCount;
import com.liveqa.repository.projection.VoterOptionId;

/**
 * poll_responsesテーブルを操作するRepository
 * PollVoteRepository
 * @author ichikura
 */
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    /**
     * 今のセッションが選んだ行を返すメソッド
     * 二重回答の判定とmyOptionIdsを1回のSQLで兼ねる
     * @param pollId ポールid
     * @param voterKey g:{guestId}またはu:{userId}
     * @return 回答行
     */
    List<PollVote> findByPoll_IdAndVoterKey(Long pollId, String voterKey);

    /**
     * ポール配下の得票を選択肢ごとに1回のSQLで返すメソッド
     * 選択肢ごとにCOUNTを投げるとN+1になるためGROUP BYで畳む
     * 0票の選択肢は行が出ない（呼び出し側で0埋め）
     * @param pollId ポールid
     * @return 選択肢ごとの得票
     */
    @Query("""
            SELECT new com.liveqa.repository.projection.OptionVoteCount(
                v.pollOption.id, COUNT(v))
            FROM PollVote v
            WHERE v.poll.id = :pollId
            GROUP BY v.pollOption.id
            """)
    List<OptionVoteCount> countByOptionId(@Param("pollId") Long pollId);

    /**
     * イベント配下の得票を選択肢ごとに1回のSQLで返すメソッド
     * 一覧でポールごとにCOUNTするとN+1になるためイベント単位でGROUP BYする
     * 0票の選択肢は行が出ない（呼び出し側で0埋め）
     * @param eventId イベントid
     * @return 選択肢ごとの得票
     */
    @Query("""
            SELECT new com.liveqa.repository.projection.OptionVoteCount(
                v.pollOption.id, COUNT(v))
            FROM PollVote v
            WHERE v.poll.event.id = :eventId
            GROUP BY v.pollOption.id
            """)
    List<OptionVoteCount> countByOptionIdForEvent(@Param("eventId") Long eventId);

    /**
     * 今のセッションがイベント配下で選んだ選択肢を1回のSQLで返すメソッド
     * 一覧の myOptionIds 用。ポールごとにSELECTするとN+1になる
     * @param eventId イベントid
     * @param voterKey g:{guestId}またはu:{userId}
     * @return ポールidと選択肢id
     */
    @Query("""
            SELECT new com.liveqa.repository.projection.VoterOptionId(
                v.poll.id, v.pollOption.id)
            FROM PollVote v
            WHERE v.poll.event.id = :eventId
              AND v.voterKey = :voterKey
            """)
    List<VoterOptionId> findVoterOptionIdsByEventId(
            @Param("eventId") Long eventId,
            @Param("voterKey") String voterKey);
}
