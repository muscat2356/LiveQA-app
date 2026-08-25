package com.liveqa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.dto.response.GuestResponse;
import com.liveqa.service.GuestSessionService;

/**
 * Guest POST の CSRF（Filter 層）。
 * 発行口は ignore。あり／なしどちらも 200。他 path の CSRF は見ない。
 */
@WebMvcTest(
        controllers = GuestSessionController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import(GuestSessionControllerCsrfTest.GuestPermitCsrfSecurityConfig.class)
class GuestSessionControllerCsrfTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GuestSessionService guestSessionService;

    @BeforeEach
    void stubIssueOrReuse() {
        when(guestSessionService.issueOrReuse(any()))
                .thenReturn(new GuestResponse("new-id", "g:new-id"));
    }

    @Test
    void createGuestSession_CSRFありなら200() throws Exception {
        mockMvc.perform(post("/api/auth/guest").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestId").value("new-id"))
                .andExpect(jsonPath("$.voterKey").value("g:new-id"));
    }

    @Test
    void createGuestSession_CSRFなしでも200() throws Exception {
        mockMvc.perform(post("/api/auth/guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestId").value("new-id"))
                .andExpect(jsonPath("$.voterKey").value("g:new-id"));
    }

    /**
     * 本番 SecurityConfig は Import しない（OAuth2 依存を避ける）。
     * guest の ignore だけ本番と同じ matcher で再現する。
     */
    static class GuestPermitCsrfSecurityConfig {

        @Bean
        SecurityFilterChain guestPermitCsrfFilterChain(HttpSecurity http) throws Exception {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/guest"));
            return http.build();
        }
    }
}
