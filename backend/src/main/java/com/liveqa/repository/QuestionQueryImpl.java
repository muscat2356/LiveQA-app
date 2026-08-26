package com.liveqa.repository;

import java.util.List;
import java.util.Optional;

import com.liveqa.domain.Question;
import com.liveqa.enums.QuestionSort;
import com.liveqa.enums.QuestionStatus;
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
     * イベント配下の質問を並び・status指定で返すメソッド
     * WHERE は event.id。未指定のstatusはARCHIVED以外。並びは sort
     * @param eventId イベントid
     * @param sort 並び
     * @param status 指定があればその値、emptyはARCHIVED以外
     * @return 質問一覧
     */
    @Override
    public List<Question> searchByEventId(
            Long eventId,
            QuestionSort sort,
            Optional<QuestionStatus> status
    ) {
        final BooleanBuilder where = new BooleanBuilder();
        where.and(question.event.id.eq(eventId));
        if (status.isEmpty()) {
            where.and(question.status.ne(QuestionStatus.ARCHIVED));
        } else {
            where.and(question.status.eq(status.get()));
        }

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
