package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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

    @Mock
    UserService userService;

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
        when(eventRepository.incrementQuestionCount(10L)).thenReturn(1);

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
        verify(voterKeyResolver).require(any());
        verify(questionRepository).save(any(Question.class));
        verify(eventRepository).incrementQuestionCount(10L);
        verify(eventRepository, never()).save(event);
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
        when(eventRepository.incrementQuestionCount(10L)).thenReturn(1);

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
    void create_イベントが無ければ404() {
        // 行が無ければempty。非OPENの409にはしない
        when(eventRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.create(
                10L,
                new QuestionCreateRequest("まだ？", null),
                new VoterKeySource.Guest("abc")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getMessage()).contains("見つかりません");
                });
        verify(questionRepository, never()).save(any());
        verify(eventRepository, never()).incrementQuestionCount(any());
    }

    @Test
    void findByEventId_票があればvotedByMeがtrue() {
        final Event event = openEvent(10L);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(voterKeyResolver.tryResolve(any()))
                .thenReturn(Optional.of(VoterKey.guest("abc")));

        final Question voted = Question.post(event, "資料は？", "花子");
        final Question notVoted = Question.post(event, "場所は？", "匿名");
        ReflectionTestUtils.setField(voted, "id", 42L);
        ReflectionTestUtils.setField(notVoted, "id", 43L);
        when(questionRepository.searchByEventId(
                10L, QuestionSort.POPULAR, Optional.empty()))
                .thenReturn(List.of(voted, notVoted));
        when(voteRepository.findByQuestion_Event_IdAndVoterKey(10L, "g:abc"))
                .thenReturn(List.of(Vote.cast(voted, "g:abc")));

        final List<QuestionResponse> responses = questionService.findByEventId(
                10L,
                null,
                null,
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
        final Event event = openEvent(10L);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());

        final Question question = Question.post(event, "資料は？", "花子");
        ReflectionTestUtils.setField(question, "id", 42L);
        when(questionRepository.searchByEventId(
                10L, QuestionSort.POPULAR, Optional.empty()))
                .thenReturn(List.of(question));

        final List<QuestionResponse> responses = questionService.findByEventId(
                10L,
                null,
                null,
                null,
                new VoterKeySource.Missing());

        assertThat(responses.get(0).votedByMe()).isFalse();
        verify(voteRepository, never()).findByQuestion_Event_IdAndVoterKey(any(), any());
    }

    @Test
    void findByEventId_イベント無しなら404() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.findByEventId(
                99L,
                null,
                null,
                null,
                new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
    }

    @Test
    void findByEventId_無ければ404() {
        // 無いイベントの一覧は404
        when(eventRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.findByEventId(
                10L,
                null,
                null,
                null,
                new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
    }

    @Test
    void findByEventId_newestならNEWESTで検索() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(openEvent(10L)));
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());
        when(questionRepository.searchByEventId(
                10L, QuestionSort.NEWEST, Optional.empty()))
                .thenReturn(List.of());

        questionService.findByEventId(
                10L, "newest", null, null, new VoterKeySource.Missing());

        verify(questionRepository).searchByEventId(
                10L, QuestionSort.NEWEST, Optional.empty());
    }

    @Test
    void findByEventId_不正sortなら400() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(openEvent(10L)));

        assertThatThrownBy(() -> questionService.findByEventId(
                10L,
                "oldest",
                null,
                null,
                new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("popular / newest");
                });
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
    }

    @Test
    void findByEventId_status指定ならその値で検索() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(openEvent(10L)));
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());
        when(questionRepository.searchByEventId(
                10L, QuestionSort.POPULAR, Optional.of(QuestionStatus.OPEN)))
                .thenReturn(List.of());

        questionService.findByEventId(
                10L, null, "OPEN", null, new VoterKeySource.Missing());

        verify(questionRepository).searchByEventId(
                10L, QuestionSort.POPULAR, Optional.of(QuestionStatus.OPEN));
        verify(eventRepository, never()).findByIdAndHost_Id(any(), any());
    }

    @Test
    void findByEventId_不正statusなら400() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(openEvent(10L)));

        assertThatThrownBy(() -> questionService.findByEventId(
                10L, null, "INVALID", null, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("OPEN / ANSWERED / ARCHIVED");
                });
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
    }

    @Test
    void findByEventId_ARCHIVEDをゲストが見ると403() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(openEvent(10L)));

        assertThatThrownBy(() -> questionService.findByEventId(
                10L, null, "ARCHIVED", null, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("主催者のみ");
                });
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
        verify(userService, never()).findByOidcSubject(any());
        verify(eventRepository, never()).findByIdAndHost_Id(any(), any());
    }

    @Test
    void findByEventId_ARCHIVEDを所有者なら検索する() {
        final Event event = openEvent(10L);
        final User host = event.getHost();
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userService.findByOidcSubject("sub-1")).thenReturn(Optional.of(host));
        when(eventRepository.findByIdAndHost_Id(10L, 1L)).thenReturn(Optional.of(event));
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());
        when(questionRepository.searchByEventId(
                10L, QuestionSort.POPULAR, Optional.of(QuestionStatus.ARCHIVED)))
                .thenReturn(List.of());

        questionService.findByEventId(
                10L, null, "ARCHIVED", oidc, new VoterKeySource.Missing());

        verify(questionRepository).searchByEventId(
                10L, QuestionSort.POPULAR, Optional.of(QuestionStatus.ARCHIVED));
    }

    @Test
    void findByEventId_ARCHIVEDを他HOSTなら403() {
        final Event event = openEvent(10L);
        final User other = User.hostOf("sub-2");
        ReflectionTestUtils.setField(other, "id", 2L);
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-2");
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userService.findByOidcSubject("sub-2")).thenReturn(Optional.of(other));
        when(eventRepository.findByIdAndHost_Id(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.findByEventId(
                10L, null, "ARCHIVED", oidc, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("主催者のみ");
                });
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
    }

    @Test
    void findByEventId_ARCHIVEDでusers行が無ければ403() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(openEvent(10L)));
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-ghost");
        when(userService.findByOidcSubject("sub-ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.findByEventId(
                10L, null, "ARCHIVED", oidc, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getMessage()).contains("主催者のみ");
                });
        verify(eventRepository, never()).findByIdAndHost_Id(any(), any());
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
    }

    @Test
    void findByEventId_ARCHIVEDでもイベント無しは404() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.findByEventId(
                99L, null, "ARCHIVED", null, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(userService, never()).findByOidcSubject(any());
        verify(questionRepository, never()).searchByEventId(any(), any(), any());
    }

    @Test
    void updateStatus_OPENからANSWEREDへ() {
        final Event event = openEvent(10L);
        final Question question = Question.post(event, "資料は？", "花子");
        ReflectionTestUtils.setField(question, "id", 42L);
        final User host = event.getHost();
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(host);
        when(questionRepository.findByIdAndEvent_Host_Id(42L, 1L))
                .thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());

        final QuestionResponse response = questionService.updateStatus(
                42L,
                QuestionStatus.ANSWERED,
                oidc,
                new VoterKeySource.Missing());

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(QuestionStatus.ANSWERED);
        assertThat(response.votedByMe()).isFalse();
        assertThat(question.getStatus()).isEqualTo(QuestionStatus.ANSWERED);
        verify(questionRepository).save(question);
    }

    @Test
    void updateStatus_ARCHIVEDからOPENは409() {
        final Event event = openEvent(10L);
        final Question question = Question.post(event, "資料は？", "花子");
        ReflectionTestUtils.setField(question, "id", 42L);
        question.changeStatus(QuestionStatus.ARCHIVED);
        final User host = event.getHost();
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(host);
        when(questionRepository.findByIdAndEvent_Host_Id(42L, 1L))
                .thenReturn(Optional.of(question));

        assertThatThrownBy(() -> questionService.updateStatus(
                42L,
                QuestionStatus.OPEN,
                oidc,
                new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("この状態遷移はできません");
                });
        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateStatus_他人所有なら404() {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(host);
        when(questionRepository.findByIdAndEvent_Host_Id(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.updateStatus(
                99L,
                QuestionStatus.ANSWERED,
                oidc,
                new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(questionRepository, never()).save(any());
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
