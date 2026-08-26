package com.liveqa.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.request.PollCreateRequest;
import com.liveqa.dto.request.PollRespondRequest;
import com.liveqa.dto.response.PollOptionResponse;
import com.liveqa.dto.response.PollResponse;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;
import com.liveqa.exception.GlobalExceptionHandler;
import com.liveqa.service.PollService;

/**
 * PollController の HTTP 面（作成・一覧・開閉・回答）。
 * 本番 SecurityConfig は Import せず、@PreAuthorize と csrf() で HOST / CSRF を分ける。
 */
@WebMvcTest(
        controllers = PollController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import({ PollControllerTest.MethodSecurityTestConfig.class, GlobalExceptionHandler.class })
class PollControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PollService pollService;

    @Test
    void create_HOSTなら201() throws Exception {
        when(pollService.create(eq(10L), any(PollCreateRequest.class), any(OidcUser.class)))
                .thenReturn(new PollResponse(
                        5L,
                        10L,
                        "次のLTで聞きたいテーマは？",
                        PollType.SINGLE,
                        PollStatus.DRAFT,
                        List.of(
                                new PollOptionResponse(31L, "AI", 0),
                                new PollOptionResponse(32L, "Web", 0)
                        ),
                        0,
                        List.of()
                ));

        mockMvc.perform(post("/api/events/10/polls")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionText":"次のLTで聞きたいテーマは？","type":"SINGLE","options":["AI","Web"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.eventId").value(10))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalResponses").value(0));
    }

    @Test
    void create_HOST以外なら403() throws Exception {
        mockMvc.perform(post("/api/events/10/polls")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionText":"次のLTで聞きたいテーマは？","type":"SINGLE","options":["AI","Web"]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_CSRFなしなら403() throws Exception {
        mockMvc.perform(post("/api/events/10/polls")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionText":"次のLTで聞きたいテーマは？","type":"SINGLE","options":["AI","Web"]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByEventId_HOSTなら200() throws Exception {
        when(pollService.findByEventId(eq(10L), any(OidcUser.class), any()))
                .thenReturn(List.of(new PollResponse(
                        5L,
                        10L,
                        "次のテーマは？",
                        PollType.SINGLE,
                        PollStatus.DRAFT,
                        List.of(new PollOptionResponse(31L, "AI", 0)),
                        0,
                        List.of()
                )));

        mockMvc.perform(get("/api/events/10/polls").with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                        .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void findByEventId_ゲストならsourceを渡す() throws Exception {
        when(pollService.findByEventId(eq(10L), eq(null), any()))
                .thenReturn(List.of());

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(get("/api/events/10/polls").session(session))
                .andExpect(status().isOk());

        final ArgumentCaptor<VoterKeySource> captor =
                ArgumentCaptor.forClass(VoterKeySource.class);
        verify(pollService).findByEventId(eq(10L), eq(null), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new VoterKeySource.Guest("abc"));
    }

    @Test
    void findByEventId_未ログインなら200() throws Exception {
        when(pollService.findByEventId(eq(10L), eq(null), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/events/10/polls"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_HOSTなら200() throws Exception {
        when(pollService.updateStatus(eq(5L), eq(PollStatus.OPEN), any(OidcUser.class), any()))
                .thenReturn(new PollResponse(
                        5L,
                        10L,
                        "次のテーマは？",
                        PollType.SINGLE,
                        PollStatus.OPEN,
                        List.of(new PollOptionResponse(31L, "AI", 0)),
                        0,
                        List.of()
                ));

        mockMvc.perform(patch("/api/polls/5/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void updateStatus_HOST以外なら403() throws Exception {
        mockMvc.perform(patch("/api/polls/5/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_CSRFなしなら403() throws Exception {
        mockMvc.perform(patch("/api/polls/5/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_typeが不正なら400() throws Exception {
        mockMvc.perform(post("/api/events/10/polls")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionText":"次のLTで聞きたいテーマは？","type":"INVALID","options":["AI","Web"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("リクエストの形式が正しくありません。"));
    }

    @Test
    void updateStatus_statusが不正なら400() throws Exception {
        mockMvc.perform(patch("/api/polls/5/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("リクエストの形式が正しくありません。"));
    }

    @Test
    void respond_ゲストなら200() throws Exception {
        when(pollService.respond(eq(5L), any(PollRespondRequest.class), any()))
                .thenReturn(new PollResponse(
                        5L,
                        10L,
                        "次のテーマは？",
                        PollType.SINGLE,
                        PollStatus.OPEN,
                        List.of(
                                new PollOptionResponse(31L, "AI", 1),
                                new PollOptionResponse(32L, "Web", 0)
                        ),
                        1,
                        List.of(31L)
                ));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(post("/api/polls/5/responses")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionIds\":[31]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(1))
                .andExpect(jsonPath("$.myOptionIds[0]").value(31));

        final ArgumentCaptor<VoterKeySource> captor =
                ArgumentCaptor.forClass(VoterKeySource.class);
        verify(pollService).respond(eq(5L), any(PollRespondRequest.class), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new VoterKeySource.Guest("abc"));
    }

    @Test
    void respond_CSRFなしなら403() throws Exception {
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(post("/api/polls/5/responses")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionIds\":[31]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findResults_未ログインなら200() throws Exception {
        when(pollService.findResults(eq(5L), eq(null), any()))
                .thenReturn(new PollResponse(
                        5L,
                        10L,
                        "次のテーマは？",
                        PollType.SINGLE,
                        PollStatus.OPEN,
                        List.of(
                                new PollOptionResponse(31L, "AI", 2),
                                new PollOptionResponse(32L, "Web", 1)
                        ),
                        3,
                        List.of()
                ));

        mockMvc.perform(get("/api/polls/5/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(3))
                .andExpect(jsonPath("$.options[0].count").value(2))
                .andExpect(jsonPath("$.myOptionIds").isEmpty());
    }

    @Test
    void findResults_ゲストのSessionをSourceに渡す() throws Exception {
        when(pollService.findResults(eq(5L), eq(null), any()))
                .thenReturn(new PollResponse(
                        5L,
                        10L,
                        "次のテーマは？",
                        PollType.SINGLE,
                        PollStatus.OPEN,
                        List.of(new PollOptionResponse(31L, "AI", 1)),
                        1,
                        List.of(31L)
                ));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(get("/api/polls/5/results").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myOptionIds[0]").value(31));

        final ArgumentCaptor<VoterKeySource> captor =
                ArgumentCaptor.forClass(VoterKeySource.class);
        verify(pollService).findResults(eq(5L), eq(null), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new VoterKeySource.Guest("abc"));
    }

    /**
     * GET は未ログイン可。POST / PATCH の CSRF / @PreAuthorize は残す。
     */
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {

        @Bean
        SecurityFilterChain pollTestFilterChain(HttpSecurity http) throws Exception {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(Customizer.withDefaults());
            return http.build();
        }
    }
}
