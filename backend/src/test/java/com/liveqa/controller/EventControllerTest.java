package com.liveqa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.domain.Event;
import com.liveqa.dto.response.EventCreateResponse;
import com.liveqa.dto.response.EventDetailResponse;
import com.liveqa.dto.response.EventStatusResponse;
import com.liveqa.dto.response.EventSummaryResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.exception.GlobalExceptionHandler;
import com.liveqa.service.EventService;

/**
 * EventController の HTTP 面を検証する。
 * 本番 SecurityConfig は Import せず、@PreAuthorize と csrf() で HOST / CSRF を分ける。
 */
@WebMvcTest(
        controllers = EventController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import({ EventControllerTest.MethodSecurityTestConfig.class, GlobalExceptionHandler.class })
class EventControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EventService eventService;

    @Test
    void findAll_HOSTなら200() throws Exception {
        final Instant createdAt = Instant.parse("2026-08-07T05:00:00Z");
        when(eventService.findAll(any(OidcUser.class)))
                .thenReturn(List.of(new EventSummaryResponse(
                        10L, "第一回", "Ab12Cd34", EventStatus.DRAFT, createdAt)));

        mockMvc.perform(get("/api/events").with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                        .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("第一回"))
                .andExpect(jsonPath("$[0].joinCode").value("Ab12Cd34"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].questionCount").doesNotExist());
    }

    @Test
    void findAll_HOST以外なら403() throws Exception {
        mockMvc.perform(get("/api/events").with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_HOSTなら201() throws Exception {
        final Instant createdAt = Instant.parse("2026-08-07T05:00:00Z");
        when(eventService.save(any(OidcUser.class), eq("curlテストイベント")))
                .thenReturn(new EventCreateResponse(
                        20L,
                        "curlテストイベント",
                        "Xy9kLm2n",
                        "/join/Xy9kLm2n",
                        EventStatus.DRAFT,
                        createdAt));

        mockMvc.perform(post("/api/events")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"curlテストイベント\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.title").value("curlテストイベント"))
                .andExpect(jsonPath("$.joinCode").value("Xy9kLm2n"))
                .andExpect(jsonPath("$.joinUrl").value("/join/Xy9kLm2n"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void create_HOST以外なら403() throws Exception {
        mockMvc.perform(post("/api/events")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findById_HOSTなら200() throws Exception {
        final Instant t = Instant.parse("2026-08-07T05:00:00Z");
        when(eventService.findById(eq(10L), any(OidcUser.class)))
                .thenReturn(new EventDetailResponse(
                        10L,
                        "第一回",
                        "Ab12Cd34",
                        "/join/Ab12Cd34",
                        EventStatus.DRAFT,
                        12,
                        t,
                        t));

        mockMvc.perform(get("/api/events/10").with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                        .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.questionCount").value(12))
                .andExpect(jsonPath("$.joinUrl").value("/join/Ab12Cd34"));
    }

    @Test
    void findById_HOST以外なら403() throws Exception {
        mockMvc.perform(get("/api/events/10").with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_HOSTなら200() throws Exception {
        when(eventService.updateStatus(eq(10L), eq(EventStatus.OPEN), any(OidcUser.class)))
                .thenReturn(new EventStatusResponse(10L, EventStatus.OPEN));

        mockMvc.perform(patch("/api/events/10/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void updateStatus_HOST以外なら403() throws Exception {
        mockMvc.perform(patch("/api/events/10/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_楽観ロック衝突なら409() throws Exception {
        when(eventService.updateStatus(eq(10L), eq(EventStatus.OPEN), any(OidcUser.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Event.class, 10L));

        mockMvc.perform(patch("/api/events/10/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void updateTitle_HOSTなら200() throws Exception {
        final Instant t = Instant.parse("2026-08-07T05:00:00Z");
        when(eventService.updateTitle(eq(10L), eq("修正後"), any(OidcUser.class)))
                .thenReturn(new EventDetailResponse(
                        10L,
                        "修正後",
                        "Ab12Cd34",
                        "/join/Ab12Cd34",
                        EventStatus.DRAFT,
                        12,
                        t,
                        t));

        mockMvc.perform(patch("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"修正後\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("修正後"))
                .andExpect(jsonPath("$.joinCode").value("Ab12Cd34"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void updateTitle_HOST以外なら403() throws Exception {
        mockMvc.perform(patch("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"修正後\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTitle_CSRFなしなら403() throws Exception {
        mockMvc.perform(patch("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"修正後\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTitle_楽観ロック衝突なら409() throws Exception {
        when(eventService.updateTitle(eq(10L), eq("修正後"), any(OidcUser.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Event.class, 10L));

        mockMvc.perform(patch("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"修正後\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("他の操作と衝突しました。画面を更新して再度お試しください。"));
    }

    @Test
    void delete_HOSTなら204() throws Exception {
        mockMvc.perform(delete("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_HOST以外なら403() throws Exception {
        mockMvc.perform(delete("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_CSRFなしなら403() throws Exception {
        mockMvc.perform(delete("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_楽観ロック衝突なら409() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Event.class, 10L))
                .when(eventService).delete(eq(10L), any(OidcUser.class));

        mockMvc.perform(delete("/api/events/10")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
