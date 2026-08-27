package com.liveqa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.QuestionStatus;
import com.liveqa.service.QuestionService;
import com.liveqa.websocket.QuestionEventPublisher;
import com.liveqa.websocket.QuestionEventType;

/**
 * 質問ステータス PATCH の HTTP 面（HOST / CSRF）。
 * 本番 SecurityConfig は Import しない。
 */
@WebMvcTest(
        controllers = QuestionController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import(QuestionControllerStatusTest.MethodSecurityTestConfig.class)
class QuestionControllerStatusTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QuestionService questionService;

    @MockitoBean
    QuestionEventPublisher questionEventPublisher;

    @Test
    void updateStatus_HOSTなら200() throws Exception {
        QuestionResponse response = new QuestionResponse(
                42L,
                10L,
                "資料は？",
                "花子",
                8,
                QuestionStatus.ANSWERED,
                false,
                Instant.parse("2026-08-21T00:00:00Z"));

        when(questionService.updateStatus(
                eq(42L),
                eq(QuestionStatus.ANSWERED),
                any(OidcUser.class),
                any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/questions/42/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ANSWERED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.eventId").value(10))
                .andExpect(jsonPath("$.status").value("ANSWERED"));

        verify(questionEventPublisher).publish(
                QuestionEventType.QUESTION_ANSWERED,
                response);
    }

    @Test
    void updateStatus_HOST以外なら403() throws Exception {
        mockMvc.perform(patch("/api/questions/42/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .idToken(id -> id.subject("sub-1")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ANSWERED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_CSRFなしなら403() throws Exception {
        mockMvc.perform(patch("/api/questions/42/status")
                        .with(oidcLogin()
                                .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                                .idToken(id -> id.subject("sub-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ANSWERED\"}"))
                .andExpect(status().isForbidden());
    }

    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
