package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
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
import com.liveqa.domain.User;
import com.liveqa.dto.response.EventCreateResponse;
import com.liveqa.dto.response.EventDetailResponse;
import com.liveqa.dto.response.EventJoinResponse;
import com.liveqa.dto.response.EventStatusResponse;
import com.liveqa.dto.response.EventSummaryResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.exception.BusinessException;
import com.liveqa.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    UserService userService;

    @InjectMocks
    EventService eventService;

    @Test
    void findAll_ログインユーザーのイベントを全てDTOへ変換() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 1L);
        when(eventRepository.findByHost_IdAndStatusNotOrderByCreatedAtDesc(1L, EventStatus.DELETED))
                .thenReturn(List.of(event));

        final List<EventSummaryResponse> result = eventService.findAll(oidc);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).title()).isEqualTo("第一回");
        assertThat(result.get(0).joinCode()).isEqualTo("Ab12Cd34");
        assertThat(result.get(0).status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void save_作成してEventCreateResponseを返す() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);
        when(eventRepository.existsByJoinCode(anyString())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            final Event e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 20L);
            return e;
        });

        final EventCreateResponse response = eventService.save(oidc, "curlテストイベント");

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.title()).isEqualTo("curlテストイベント");
        assertThat(response.joinCode()).hasSize(8);
        assertThat(response.joinUrl()).isEqualTo("/join/" + response.joinCode());
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void generateJoinCode_衝突したら再発行して返す() {
        when(eventRepository.existsByJoinCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        final String code = eventService.generateJoinCode().code();

        assertThat(code).hasSize(8);
        verify(eventRepository, atLeast(2)).existsByJoinCode(anyString());
    }

    @Test
    void generateJoinCode_再発行上限でConflict() {
        when(eventRepository.existsByJoinCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> eventService.generateJoinCode())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("再生成の上限");
    }

    @Test
    void findById_所有者なら詳細DTOを返す() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        ReflectionTestUtils.setField(event, "questionCount", 12);
        when(eventRepository.findByIdAndHost_IdAndStatusNot(10L, 1L, EventStatus.DELETED)).thenReturn(Optional.of(event));

        final EventDetailResponse response = eventService.findById(10L, oidc);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.questionCount()).isEqualTo(12);
        assertThat(response.joinUrl()).isEqualTo("/join/Ab12Cd34");
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void findById_無ければ404() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);
        when(eventRepository.findByIdAndHost_IdAndStatusNot(99L, 1L, EventStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(99L, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getMessage()).contains("見つかりません");
                });
    }

    @Test
    void findById_他人のイベントなら404() {
        final User me = User.hostOf("sub-1");
        ReflectionTestUtils.setField(me, "id", 1L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(me);
        // id+host_id で絞るため、他人所有は 0 件
        when(eventRepository.findByIdAndHost_IdAndStatusNot(10L, 1L, EventStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(10L, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void findByJoinCode_あれば紹介DTO() {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final Event event = Event.create(host, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findByJoinCodeAndStatusNot("Ab12Cd34", EventStatus.DELETED)).thenReturn(Optional.of(event));

        final EventJoinResponse response = eventService.findByJoinCode("Ab12Cd34");

        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("第一回");
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void findByJoinCode_無ければ404() {
        when(eventRepository.findByJoinCodeAndStatusNot("Ab12Cd34", EventStatus.DELETED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByJoinCode("Ab12Cd34"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getMessage()).contains("該当するルーム");
                });
    }

    @Test
    void findByJoinCode_形式不正なら400() {
        assertThatThrownBy(() -> eventService.findByJoinCode("bad!"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("参加コードが不正");
                });
    }

    @Test
    void updateStatus_DRAFTからOPENへ() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findByIdAndHost_IdAndStatusNot(10L, 1L, EventStatus.DELETED)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        final EventStatusResponse response =
                eventService.updateStatus(10L, EventStatus.OPEN, oidc);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(EventStatus.OPEN);
        assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
        verify(eventRepository).save(event);
    }

    @Test
    void updateStatus_OPENからCLOSEDへ() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        event.changeStatus(EventStatus.OPEN);
        when(eventRepository.findByIdAndHost_IdAndStatusNot(10L, 1L, EventStatus.DELETED)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        final EventStatusResponse response =
                eventService.updateStatus(10L, EventStatus.CLOSED, oidc);

        assertThat(response.status()).isEqualTo(EventStatus.CLOSED);
    }

    @Test
    void updateStatus_CLOSEDからOPENへ() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        event.changeStatus(EventStatus.OPEN);
        event.changeStatus(EventStatus.CLOSED);
        when(eventRepository.findByIdAndHost_IdAndStatusNot(10L, 1L, EventStatus.DELETED)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        final EventStatusResponse response =
                eventService.updateStatus(10L, EventStatus.OPEN, oidc);

        assertThat(response.status()).isEqualTo(EventStatus.OPEN);
    }

    @Test
    void updateStatus_DRAFTからCLOSEDは409() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final Event event = Event.create(user, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findByIdAndHost_IdAndStatusNot(10L, 1L, EventStatus.DELETED)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateStatus(10L, EventStatus.CLOSED, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("状態遷移");
                });
    }

    @Test
    void updateStatus_要求がDRAFTなら400() {
        final OidcUser oidc = mock(OidcUser.class);

        assertThatThrownBy(() -> eventService.updateStatus(10L, EventStatus.DRAFT, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("OPEN / CLOSED");
                });
    }
}
