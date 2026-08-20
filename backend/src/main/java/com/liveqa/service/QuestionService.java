package com.liveqa.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liveqa.domain.Event;
import com.liveqa.domain.Question;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.request.QuestionCreateRequest;
import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;
import com.liveqa.repository.QuestionRepository;
import com.liveqa.repository.VoteRepository;

/**
 * Questionsに対する業務処理を行うサービスクラス
 * QuestionService
 * @author ichikura
 */
@Service
public class QuestionService {

    private final EventRepository eventRepository;
    private final QuestionRepository questionRepository;
    private final VoteRepository voteRepository;
    private final VoterKeyResolver voterKeyResolver;
    private static final String ANONYMOUS_DISPLAY_NAME = "匿名";

    public QuestionService(
        EventRepository eventRepository,
        QuestionRepository questionRepository,
        VoteRepository voteRepository,
        VoterKeyResolver voterKeyResolver
    ) {
        this.eventRepository = eventRepository;
        this.questionRepository = questionRepository;
        this.voteRepository = voteRepository;
        this.voterKeyResolver = voterKeyResolver;
    }

    /**
     * 質問投稿を行うメソッド
     * 識別未確立は401
     * イベントが無ければ404、ルームがOPENでなければ409
     * 空ニックネームは「匿名」、投稿後にquestion_countを加算
     * votedByMeは投稿直後なのでfalse
     * @param eventId イベントid
     * @param request 質問本文と任意ニックネーム
     * @param source 投票キーの取得元
     * @return 作成した質問情報Responseクラス
     */
    @Transactional
    public QuestionResponse create(
        Long eventId,
        QuestionCreateRequest request,
        VoterKeySource source
    ) {

        voterKeyResolver.require(source);

        final Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "Not Found",
                    "イベントが見つかりません。"
                ));
        if(event.getStatus() != EventStatus.OPEN) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "Conflict",
                "このルームは現在質問を受け付けていません"
            );
        }

        final Question saved = questionRepository.save(
            toPostQuestion(event, request)
        );

        event.incrementQuestionCount();
        eventRepository.save(event);

        return toQuestionResponse(saved, event.getId(), false);

    }

    /**
     * イベント配下の質問一覧を返すメソッド
     * イベントが無ければ404
     * キー未確立でも200、votedByMeは全部false
     * 並びはcreated_at DESC
     * @param eventId イベントid
     * @param source 投票キーの取得元
     * @return 一覧Responseクラス
     */
    public List<QuestionResponse> findByEventId(Long eventId, VoterKeySource source) {
        if (!eventRepository.existsById(eventId)) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "イベントが見つかりません。"
            );
        }

        final Set<Long> votedQuestionIds = votedQuestionIds(eventId, source);

        return questionRepository.findByEvent_IdOrderByCreatedAtDesc(eventId)
            .stream()
            .map(question -> toQuestionResponse(
                question,
                eventId,
                votedQuestionIds.contains(question.getId())))
            .toList();

    }

    /**
     * 今のセッションがいいねした質問idの集合を返すメソッド
     * キー未確立なら空のSet（votedByMeは全部false）
     * @param eventId イベントid
     * @param source 投票キーの取得元
     * @return いいね済み質問id
     */
    private Set<Long> votedQuestionIds(Long eventId, VoterKeySource source) {
        final Optional<VoterKey> voterKey = voterKeyResolver.tryResolve(source);
        if (voterKey.isEmpty()) {
            return Set.of();
        }
        return voteRepository
            .findByQuestion_Event_IdAndVoterKey(eventId, voterKey.get().value())
            .stream()
            .map(vote -> vote.getQuestion().getId())
            .collect(Collectors.toSet());
    }

    /**
     * RequestからQuestionへ変換するメソッド
     * 空ニックネームは「匿名」
     * @param event 所属イベント
     * @param request 質問本文と任意ニックネーム
     * @return 投稿用のQuestion
     */
    private Question toPostQuestion(Event event, QuestionCreateRequest request) {
        final String body = request.body().trim();
        final String displayName = Optional.ofNullable(request.nickname())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(ANONYMOUS_DISPLAY_NAME);
        return Question.post(event, body, displayName);
    }

    /**
     * レスポンスDTOへ変換するメソッド
     * @param question DBから取得されたドメイン
     * @param eventId イベントid
     * @param votedByMe 今のセッションがいいね済みか
     * @return レスポンス
     */
    private QuestionResponse toQuestionResponse(
        Question question,
        Long eventId,
        boolean votedByMe
    ) {
        return new QuestionResponse(
            question.getId(),
            eventId,
            question.getBody(),
            question.getDisplayName(),
            question.getUpvoteCount(),
            question.getStatus(),
            votedByMe,
            question.getCreatedAt()
        );
    }

}
