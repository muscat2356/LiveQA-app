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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.liveqa.domain.Event;
import com.liveqa.domain.Poll;
import com.liveqa.domain.PollVote;
import com.liveqa.domain.User;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.request.PollCreateRequest;
import com.liveqa.dto.request.PollRespondRequest;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;
import com.liveqa.repository.PollRepository;
import com.liveqa.repository.PollVoteRepository;
import com.liveqa.repository.projection.OptionVoteCount;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    PollRepository pollRepository;

    @Mock
    EventRepository eventRepository;

    @Mock
    UserService userService;

    @Mock
    PollVoteRepository pollVoteRepository;

    @Mock
    VoterKeyResolver voterKeyResolver;

    @InjectMocks
    PollService pollService;

    @Test
    void create_DRAFTでtrim済みラベルを返す() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "LT会", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findByIdAndHost_Id(10L, 1L)).thenReturn(Optional.of(event));
        when(pollRepository.save(any(Poll.class))).thenAnswer(inv -> {
            final Poll poll = inv.getArgument(0);
            ReflectionTestUtils.setField(poll, "id", 5L);
            return poll;
        });

        final PollCreateRequest request = new PollCreateRequest(
                " 次のテーマは？ ",
                PollType.SINGLE,
                List.of(" AI", "Web ")
        );

        final PollResponse response = pollService.create(10L, request, oidc);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.questionText()).isEqualTo("次のテーマは？");
        assertThat(response.type()).isEqualTo(PollType.SINGLE);
        assertThat(response.status()).isEqualTo(PollStatus.DRAFT);
        assertThat(response.options()).extracting(o -> o.label()).containsExactly("AI", "Web");
        assertThat(response.totalResponses()).isEqualTo(0);
        assertThat(response.myOptionIds()).isEmpty();
    }

    @Test
    void create_trim後に重複なら400() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "LT会", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findByIdAndHost_Id(10L, 1L)).thenReturn(Optional.of(event));

        final PollCreateRequest request = new PollCreateRequest(
                "次のテーマは？",
                PollType.SINGLE,
                List.of("AI", " AI ")
        );

        assertThatThrownBy(() -> pollService.create(10L, request, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("重複");
                });
    }

    @Test
    void create_イベントが無ければ404() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);
        when(eventRepository.findByIdAndHost_Id(99L, 1L)).thenReturn(Optional.empty());

        final PollCreateRequest request = new PollCreateRequest(
                "次のテーマは？",
                PollType.SINGLE,
                List.of("AI", "Web")
        );

        assertThatThrownBy(() -> pollService.create(99L, request, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void findByEventId_所有者なら全ステータス() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "LT会", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findByIdAndHost_Id(10L, 1L)).thenReturn(Optional.of(event));

        final Poll draft = Poll.create(event, "下書き", PollType.SINGLE, List.of("A", "B"));
        ReflectionTestUtils.setField(draft, "id", 1L);
        final Poll open = Poll.create(event, "受付中", PollType.SINGLE, List.of("A", "B"));
        open.changeStatus(PollStatus.OPEN);
        ReflectionTestUtils.setField(open, "id", 2L);
        when(pollRepository.findByEvent_IdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(draft, open));

        final List<PollResponse> result = pollService.findByEventId(10L, oidc);

        assertThat(result).extracting(PollResponse::status)
                .containsExactly(PollStatus.DRAFT, PollStatus.OPEN);
    }

    @Test
    void findByEventId_未ログインならOPENのみ() {
        final User host = User.hostOf("sub-host");
        final Event event = Event.create(host, "LT会", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        final Poll open = Poll.create(event, "受付中", PollType.SINGLE, List.of("A", "B"));
        open.changeStatus(PollStatus.OPEN);
        ReflectionTestUtils.setField(open, "id", 2L);
        when(pollRepository.findByEvent_IdAndStatusOrderByCreatedAtAsc(10L, PollStatus.OPEN))
                .thenReturn(List.of(open));

        final List<PollResponse> result = pollService.findByEventId(10L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(PollStatus.OPEN);
    }

    @Test
    void findByEventId_イベントが無ければ404() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.findByEventId(99L, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void updateStatus_DRAFTからOPEN() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "LT会", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        final Poll poll = Poll.create(event, "次のテーマは？", PollType.SINGLE, List.of("A", "B"));
        ReflectionTestUtils.setField(poll, "id", 5L);
        when(pollRepository.findByIdAndEvent_Host_Id(5L, 1L)).thenReturn(Optional.of(poll));
        when(pollRepository.save(poll)).thenReturn(poll);

        final PollResponse response = pollService.updateStatus(5L, PollStatus.OPEN, oidc);

        assertThat(response.status()).isEqualTo(PollStatus.OPEN);
        assertThat(response.id()).isEqualTo(5L);
    }

    @Test
    void updateStatus_DRAFTを送ると400() {
        final OidcUser oidc = mock(OidcUser.class);

        assertThatThrownBy(() -> pollService.updateStatus(5L, PollStatus.DRAFT, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("OPEN / CLOSED");
                });
    }

    @Test
    void updateStatus_CLOSEDからOPENは409() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "LT会", "Ab12Cd34");
        final Poll poll = Poll.create(event, "次のテーマは？", PollType.SINGLE, List.of("A", "B"));
        poll.changeStatus(PollStatus.OPEN);
        poll.changeStatus(PollStatus.CLOSED);
        ReflectionTestUtils.setField(poll, "id", 5L);
        when(pollRepository.findByIdAndEvent_Host_Id(5L, 1L)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.updateStatus(5L, PollStatus.OPEN, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("状態遷移");
                });
    }

    @Test
    void updateStatus_無ければ404() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);
        when(pollRepository.findByIdAndEvent_Host_Id(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.updateStatus(99L, PollStatus.OPEN, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void respond_SINGLEなら200でcountとmyOptionIds() {
        final Poll poll = openPoll(5L, PollType.SINGLE);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(pollRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPoll_IdAndVoterKey(5L, "g:abc")).thenReturn(List.of());
        when(pollVoteRepository.saveAllAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pollVoteRepository.countByOptionId(5L))
                .thenReturn(List.of(new OptionVoteCount(31L, 1L)));

        final PollResponse response = pollService.respond(
                5L,
                new PollRespondRequest(List.of(31L)),
                new VoterKeySource.Guest("abc")
        );

        assertThat(response.totalResponses()).isEqualTo(1);
        assertThat(response.myOptionIds()).containsExactly(31L);
        assertThat(response.options()).extracting(o -> o.count()).containsExactly(1, 0);
        verify(pollRepository).findByIdForUpdate(5L);
        verify(pollRepository, never()).findById(any());
        verify(pollVoteRepository).saveAllAndFlush(any());
    }

    @Test
    void respond_二重なら409() {
        final Poll poll = openPoll(5L, PollType.SINGLE);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(pollRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPoll_IdAndVoterKey(5L, "g:abc"))
                .thenReturn(List.of(PollVote.cast(poll.getOptions().get(0), "g:abc")));

        assertThatThrownBy(() -> pollService.respond(
                5L,
                new PollRespondRequest(List.of(31L)),
                new VoterKeySource.Guest("abc")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("既に回答済み");
                });
        verify(pollVoteRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void respond_未確立なら401() {
        when(voterKeyResolver.resolve(any())).thenThrow(new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "匿名セッションが未確立です。"));

        assertThatThrownBy(() -> pollService.respond(
                5L,
                new PollRespondRequest(List.of(31L)),
                new VoterKeySource.Missing()
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
        verify(pollRepository, never()).findByIdForUpdate(any());
        verify(pollVoteRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void respond_ポールが無ければ404() {
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(pollRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.respond(
                99L,
                new PollRespondRequest(List.of(31L)),
                new VoterKeySource.Guest("abc")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(pollVoteRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void respond_ポールがOPENでなければ409() {
        final Poll poll = openPoll(5L, PollType.SINGLE);
        poll.changeStatus(PollStatus.CLOSED);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(pollRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.respond(
                5L,
                new PollRespondRequest(List.of(31L)),
                new VoterKeySource.Guest("abc")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("受け付けていません");
                });
        verify(pollVoteRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void respond_UNIQUE違反なら409() {
        final Poll poll = openPoll(5L, PollType.SINGLE);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(pollRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPoll_IdAndVoterKey(5L, "g:abc")).thenReturn(List.of());
        when(pollVoteRepository.saveAllAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uq_poll_responses_option_voter"));

        assertThatThrownBy(() -> pollService.respond(
                5L,
                new PollRespondRequest(List.of(31L)),
                new VoterKeySource.Guest("abc")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("既に回答済み");
                });
    }

    @Test
    void respond_MULTIで同じ選択肢を選び直すと409() {
        final Poll poll = openPoll(5L, PollType.MULTI);
        when(voterKeyResolver.resolve(any())).thenReturn(VoterKey.guest("abc"));
        when(pollRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPoll_IdAndVoterKey(5L, "g:abc"))
                .thenReturn(List.of(PollVote.cast(poll.getOptions().get(0), "g:abc")));

        assertThatThrownBy(() -> pollService.respond(
                5L,
                new PollRespondRequest(List.of(31L, 32L)),
                new VoterKeySource.Guest("abc")
        ))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("既に回答済み");
                });
        verify(pollVoteRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void findResults_OPENならキー未確立でも200() {
        final Poll poll = openPoll(5L, PollType.SINGLE);
        when(pollRepository.findById(5L)).thenReturn(Optional.of(poll));
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.empty());
        when(pollVoteRepository.countByOptionId(5L)).thenReturn(List.of(
                new OptionVoteCount(31L, 2L),
                new OptionVoteCount(32L, 1L)));

        final PollResponse response = pollService.findResults(
                5L, null, new VoterKeySource.Missing());

        assertThat(response.totalResponses()).isEqualTo(3);
        assertThat(response.myOptionIds()).isEmpty();
        assertThat(response.options()).extracting(o -> o.count()).containsExactly(2, 1);
        verify(pollVoteRepository, never()).findByPoll_IdAndVoterKey(any(), any());
    }

    @Test
    void findResults_ゲストはDRAFTを見られず404() {
        final Poll poll = draftPoll(5L);
        when(pollRepository.findById(5L)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.findResults(
                5L, null, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verify(pollVoteRepository, never()).countByOptionId(any());
    }

    @Test
    void findResults_所有者はDRAFTも200() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        final Poll poll = draftPoll(5L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);
        when(pollRepository.findById(5L)).thenReturn(Optional.of(poll));
        when(pollRepository.findByIdAndEvent_Host_Id(5L, 1L)).thenReturn(Optional.of(poll));
        when(voterKeyResolver.tryResolve(any())).thenReturn(Optional.of(VoterKey.host(1L)));
        when(pollVoteRepository.countByOptionId(5L)).thenReturn(List.of());
        when(pollVoteRepository.findByPoll_IdAndVoterKey(5L, "u:1")).thenReturn(List.of());

        final PollResponse response = pollService.findResults(
                5L, oidc, new VoterKeySource.Host("sub-1"));

        assertThat(response.status()).isEqualTo(PollStatus.DRAFT);
        assertThat(response.totalResponses()).isZero();
        assertThat(response.options()).extracting(o -> o.count()).containsExactly(0, 0);
    }

    @Test
    void findResults_ポールが無ければ404() {
        when(pollRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.findResults(
                99L, null, new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    private static Poll openPoll(Long pollId, PollType type) {
        final User host = User.hostOf("sub-1");
        final Event event = Event.create(host, "LT会", "Ab12Cd34");
        event.changeStatus(EventStatus.OPEN);
        final Poll poll = Poll.create(event, "次のテーマは？", type, List.of("AI", "Web"));
        poll.changeStatus(PollStatus.OPEN);
        ReflectionTestUtils.setField(poll, "id", pollId);
        ReflectionTestUtils.setField(poll.getOptions().get(0), "id", 31L);
        ReflectionTestUtils.setField(poll.getOptions().get(1), "id", 32L);
        return poll;
    }

    private static Poll draftPoll(Long pollId) {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final Event event = Event.create(host, "LT会", "Ab12Cd34");
        final Poll poll = Poll.create(
                event, "次のテーマは？", PollType.SINGLE, List.of("AI", "Web"));
        ReflectionTestUtils.setField(poll, "id", pollId);
        ReflectionTestUtils.setField(poll.getOptions().get(0), "id", 31L);
        ReflectionTestUtils.setField(poll.getOptions().get(1), "id", 32L);
        return poll;
    }
}
