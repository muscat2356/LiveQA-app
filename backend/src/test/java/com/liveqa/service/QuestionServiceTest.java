package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.liveqa.domain.Event;
import com.liveqa.domain.Question;
import com.liveqa.domain.User;
import com.liveqa.domain.Vote;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.request.QuestionCreateRequest;
import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.enums.QuestionSort;
import com.liveqa.enums.QuestionStatus;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;
import com.liveqa.repository.QuestionRepository;
import com.liveqa.repository.VoteRepository;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    QuestionRepository questionRepository;

    @Mock
    VoterKeyResolver voterKeyResolver;

    @Mock
    VoteRepository voteRepository;

    @InjectMocks
    QuestionService questionService;

    @Test
    void create_OPENなら保存して件数を足す() {
        final Event event = openEvent(10L);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            final Question q = inv.getArgument(0);
            ReflectionTestUtils.setField(q, "id", 42L);
            return q;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        final QuestionResponse response = questionService.create(
                10L,
                new QuestionCreateRequest("資料は公開されますか？", "花子"),
                new VoterKeySource.Guest("abc"));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.body()).isEqualTo("資料は公開されますか？");
        assertThat(response.displayName()).isEqualTo("花子");
        assertThat(response.upvoteCount()).isZero();
        assertThat(response.status()).isEqualTo(QuestionStatus.OPEN);
        assertThat(response.votedByMe()).isFalse();
        assertThat(event.getQuestionCount()).isEqualTo(1);
        verify(voterKeyResolver).require(any());
        verify(questionRepository).save(any(Question.class));
        verify(eventRepository).save(event);
    }

    @Test
    void create_nicknameなしなら匿名で保存() {
        final Event event = openEvent(10L);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            final Question q = inv.getArgument(0);
            ReflectionTestUtils.setField(q, "id", 43L);
            return q;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        final QuestionResponse response = questionService.create(
                10L,
                new QuestionCreateRequest("まだ？", null),
                new VoterKeySource.Guest("abc"));

        assertThat(response.displayName()).isEqualTo("匿名");
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void create_非OPENなら409() {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final Event event = Event.create(host, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> questionService.create(
                10L,
                new QuestionCreateRequest("まだ？", null),
                new VoterKeySource.Guest("abc")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("受け付けていません");
                });
        verify(questionRepository, never()).save(any());
    }

    @Test
    void findByEventId_票があればvotedByMeがtrue() {
        when(eventRepository.existsById(10L)).thenReturn(true);
        when(voterKeyResolver.tryResolve(any()))
                .thenReturn(Optional.of(VoterKey.guest("abc")));

        final Event event = openEvent(10L);
        final Question voted = Question.post(event, "資料は？", "花子");
        final Question notVoted = Question.post(event, "場所は？", "匿名");
        ReflectionTestUtils.setField(voted, "id", 42L);
        ReflectionTestUtils.setField(notVoted, "id", 43L);
        when(questionRepository.searchByEventId(10L, QuestionSort.POPULAR))
                .thenReturn(List.of(voted, notVoted));
        when(voteRepository.findByQuestion_Event_IdAndVoterKey(10L, "g:abc"))
                .thenReturn(List.of(Vote.cast(voted, "g:abc")));

        final List<QuestionResponse> responses = questionService.findByEventId(
                10L,
                null,
                new VoterKeySource.Guest("abc"));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(42L);
        assertThat(responses.get(0).votedByMe()).isTrue();
        assertThat(responses.get(1).id()).isEqualTo(43L);
        assertThat(responses.get(1).votedByMe()).isFalse();
    }

    @Test
    void findByEventId_キーなしなら全部false() {
        when(eventRepository.existsById(10L)).thenReturn(true);
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());

        final Event event = openEvent(10L);
        final Question question = Question.post(event, "資料は？", "花子");
        ReflectionTestUtils.setField(question, "id", 42L);
        when(questionRepository.searchByEventId(10L, QuestionSort.POPULAR))
                .thenReturn(List.of(question));

        final List<QuestionResponse> responses = questionService.findByEventId(
                10L,
                null,
                new VoterKeySource.Missing());

        assertThat(responses.get(0).votedByMe()).isFalse();
        verify(voteRepository, never()).findByQuestion_Event_IdAndVoterKey(any(), any());
    }

    @Test
    void findByEventId_イベント無しなら404() {
        when(eventRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> questionService.findByEventId(
                99L,
                null,
                new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(questionRepository, never()).searchByEventId(any(), any());
    }

    @Test
    void findByEventId_newestならNEWESTで検索() {
        when(eventRepository.existsById(10L)).thenReturn(true);
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());
        when(questionRepository.searchByEventId(10L, QuestionSort.NEWEST))
                .thenReturn(List.of());

        questionService.findByEventId(10L, "newest", new VoterKeySource.Missing());

        verify(questionRepository).searchByEventId(10L, QuestionSort.NEWEST);
    }

    @Test
    void findByEventId_不正sortなら400() {
        when(eventRepository.existsById(10L)).thenReturn(true);

        assertThatThrownBy(() -> questionService.findByEventId(
                10L,
                "oldest",
                new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("popular / newest");
                });
        verify(questionRepository, never()).searchByEventId(any(), any());
    }

    private Event openEvent(Long id) {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final Event event = Event.create(host, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", id);
        event.changeStatus(EventStatus.OPEN);
        return event;
    }
}
