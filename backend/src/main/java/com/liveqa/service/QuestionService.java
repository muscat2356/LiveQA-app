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

    @Transactional(readOnly = true)
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

    private Question toPostQuestion(Event event, QuestionCreateRequest request) {
        final String body = request.body().trim();
        final String displayName = Optional.ofNullable(request.nickname())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(ANONYMOUS_DISPLAY_NAME);
        return Question.post(event, body, displayName);
    }

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
