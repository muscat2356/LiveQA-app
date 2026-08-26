package com.liveqa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.liveqa.domain.PollVote;
import com.liveqa.repository.projection.OptionVoteCount;

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
}
