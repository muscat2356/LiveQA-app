package com.liveqa.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.liveqa.domain.Event;
import com.liveqa.domain.Question;
import com.liveqa.domain.User;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.request.QuestionCreateRequest;
import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.enums.QuestionStatus;
import com.liveqa.enums.QuestionSort;
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
    private final UserService userService;
    private static final String ANONYMOUS_DISPLAY_NAME = "匿名";

    public QuestionService(
        EventRepository eventRepository,
        QuestionRepository questionRepository,
        VoteRepository voteRepository,
        UserService userService,
        VoterKeyResolver voterKeyResolver
    ) {
        this.eventRepository = eventRepository;
        this.questionRepository = questionRepository;
        this.voteRepository = voteRepository;
        this.userService = userService;
        this.voterKeyResolver = voterKeyResolver;
    }

    /**
     * 質問投稿を行うメソッド
     * 識別未確立は401
     * イベントが無ければ404、ルームがOPENでなければ409
     * 空ニックネームは「匿名」、投稿後にquestion_countを加算
     * votedByMeは投稿直後なのでfalse
     * イベントが無ければ404（非OPENの409と区別する）
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

        // 行が無ければempty。先に404にして非OPENの409と混ぜない
        final Event event = requireVisibleEvent(eventId);
        if (event.getStatus() != EventStatus.OPEN) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "Conflict",
                "このルームは現在質問を受け付けていません"
            );
        }

        final Question saved = questionRepository.save(
            toPostQuestion(event, request)
        );

        eventRepository.incrementQuestionCount(event.getId());

        return toQuestionResponse(saved, event.getId(), false);

    }

    /**
     * イベント配下の質問一覧を返すメソッド
     * イベントが無ければ404
     * キー未確立でも200、votedByMeは全部false
     * 並びはsort（未指定はpopular、不正は400）。検索はQueryDSL
     * @param eventId イベントid
     * @param sort 並び順
     * @param source 投票キーの取得元
     * @return 一覧Responseクラス
     */
    public List<QuestionResponse> findByEventId(
        Long eventId,
        String sort,
        VoterKeySource source) {
        // 一覧も行が無ければ404（existsByIdでも同じだが、詳細と口を揃える）
        requireVisibleEvent(eventId);

        final QuestionSort questionSort;

        try {
            questionSort = QuestionSort.form(sort);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage()
            );
        }

        final Set<Long> votedQuestionIds = votedQuestionIds(eventId, source);

        return questionRepository.searchByEventId(eventId, questionSort)
            .stream()
            .map(question -> toQuestionResponse(
                question,
                eventId,
                votedQuestionIds.contains(question.getId())))
            .toList();

    }
    
    /**
     * 質問ステータスを遷移するメソッド
     * HOSTかつイベント所有者のみ。0件は404
     * 不正遷移は409
     * @param questionId 質問id
     * @param status 遷移先
     * @param oidc SecurityContext内のOidcUser
     * @param source votedByMe用（HOSTセッション）
     */
    @Transactional
    public QuestionResponse updateStatus(
        Long questionId,
        QuestionStatus status,
        OidcUser oidc,
        VoterKeySource source) {
            final User user = userService.requireByOidcSubject(oidc.getSubject());
            final Question question =
             questionRepository.findByIdAndEvent_Host_Id(questionId, user.getId())
                    .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "質問が見つかりません。"
                    ));
            
            if (!question.getStatus().canTransitionTo(status)) {
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "Conflict",
                    "この状態遷移はできません。"
                );
            }
            question.changeStatus(status);
            questionRepository.save(question);

            final boolean votedByMe = voterKeyResolver.tryResolve(source)
                .map(key -> voteRepository.existsByQuestion_IdAndVoterKey(questionId, key.value()))
                .orElse(false);
            return toQuestionResponse(question, question.getEvent().getId(), votedByMe);
        }

    /**
     * 公開してよいイベントを復元するメソッド
     * 無い行は404（存在漏洩しない）
     * @param eventId イベントid
     * @return Event
     */
    private Event requireVisibleEvent(Long eventId) {
        return eventRepository
                .findById(eventId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        "イベントが見つかりません。"));
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
