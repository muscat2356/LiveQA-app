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
        when(eventRepository.findByHost_IdOrderByCreatedAtDesc(1L))
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
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

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
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(99L, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getMessage()).contains("見つかりません");
                });
    }

    @Test
    void findById_他人のイベントなら403() {
        final User me = User.hostOf("sub-1");
        ReflectionTestUtils.setField(me, "id", 1L);
        final User other = User.hostOf("sub-2");
        ReflectionTestUtils.setField(other, "id", 2L);

        final OidcUser oidc = mock(OidcUser.class);
        when(oidc.getSubject()).thenReturn("sub-1");
        when(userService.requireByOidcSubject("sub-1")).thenReturn(me);

        final Event event = Event.create(other, "他人", "Zz99Yy88");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.findById(10L, oidc))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void findByJoinCode_あれば紹介DTO() {
        final User host = User.hostOf("sub-1");
        ReflectionTestUtils.setField(host, "id", 1L);
        final Event event = Event.create(host, "第一回", "Ab12Cd34");
        ReflectionTestUtils.setField(event, "id", 10L);
        when(eventRepository.findByJoinCode("Ab12Cd34")).thenReturn(Optional.of(event));

        final EventJoinResponse response = eventService.findByJoinCode("Ab12Cd34");

        assertThat(response.eventId()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("第一回");
        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void findByJoinCode_無ければ404() {
        when(eventRepository.findByJoinCode("Ab12Cd34")).thenReturn(Optional.empty());

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
}
