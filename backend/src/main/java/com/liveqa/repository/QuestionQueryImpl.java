package com.liveqa.repository;

import java.util.List;

import com.liveqa.domain.Question;
import com.liveqa.enums.QuestionSort;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static com.liveqa.domain.QQuestion.question;

/**
 * 質問一覧の動的クエリ実装
 * QuestionQueryImpl
 * @author ichikura
 */
public class QuestionQueryImpl implements QuestionQuery {

    private final JPAQueryFactory queryFactory;

    public QuestionQueryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * イベント配下の質問を並び指定で返すメソッド
     * WHERE は event.id。並びは sort。status は F-12
     * @param eventId イベントid
     * @param sort 並び
     * @return 質問一覧
     */
    @Override
    public List<Question> searchByEventId(Long eventId, QuestionSort sort) {
        final BooleanBuilder where = new BooleanBuilder();
        where.and(question.event.id.eq(eventId));

        return switch (sort) {
            case POPULAR -> queryFactory
                    .selectFrom(question)
                    .where(where)
                    .orderBy(question.upvoteCount.desc(), question.createdAt.desc())
                    .fetch();
            case NEWEST -> queryFactory
                    .selectFrom(question)
                    .where(where)
                    .orderBy(question.createdAt.desc())
                    .fetch();
        };
    }
}
