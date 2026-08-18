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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.liveqa.domain.Event;
import com.liveqa.domain.User;
import com.liveqa.dto.response.EventCreateResponse;
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
}
