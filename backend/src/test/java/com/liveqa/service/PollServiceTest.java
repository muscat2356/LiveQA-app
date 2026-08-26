package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.liveqa.domain.Poll;
import com.liveqa.domain.User;
import com.liveqa.dto.request.PollCreateRequest;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;
import com.liveqa.repository.PollRepository;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    PollRepository pollRepository;

    @Mock
    EventRepository eventRepository;

    @Mock
    UserService userService;

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
}
