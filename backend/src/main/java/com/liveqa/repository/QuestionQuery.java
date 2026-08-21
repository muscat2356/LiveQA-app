package com.liveqa.repository;

import java.util.List;

import com.liveqa.domain.Question;
import com.liveqa.enums.QuestionSort;

/**
 * 質問一覧の動的クエリ（QueryDSL）
 * QuestionQuery
 * @author ichikura
 */
public interface QuestionQuery {

    /**
     * イベント配下の質問を並び指定で返すメソッド
     * @param eventId イベントid
     * @param sort 並び
     * @return 質問一覧
     */
    List<Question> searchByEventId(Long eventId, QuestionSort sort);

}
