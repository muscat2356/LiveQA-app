package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import com.liveqa.domain.Question;
import com.liveqa.enums.QuestionSort;
import com.liveqa.enums.QuestionStatus;

/**
 * 質問一覧の動的クエリ（QueryDSL）
 * QuestionQuery
 * @author ichikura
 */
public interface QuestionQuery {

    /**
     * イベント配下の質問を並び・status指定で返すメソッド
     * 未指定のstatusはARCHIVED以外
     * @param eventId イベントid
     * @param sort 並び
     * @param status 指定があればその値、emptyはARCHIVED以外
     * @return 質問一覧
     */
    List<Question> searchByEventId(
            Long eventId,
            QuestionSort sort,
            Optional<QuestionStatus> status
    );

}
