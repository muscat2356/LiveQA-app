package com.liveqa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.dto.response.VoteResponse;
import com.liveqa.service.VoteResult;
import com.liveqa.service.VoteService;
import com.liveqa.websocket.QuestionEventPublisher;

/**
 * いいね POST/DELETE の CSRF（Filter 層）。
 * 配線スライス（addFilters=false）とは別に、あり→200／なし→403 を見る。
 */
@WebMvcTest(
        controllers = VoteController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import(VoteControllerCsrfTest.VotePermitCsrfSecurityConfig.class)
class VoteControllerCsrfTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    VoteService voteService;

    @MockitoBean
    QuestionEventPublisher questionEventPublisher;

    @Test
    void add_CSRFありなら200() throws Exception {
        final VoteResponse response = new VoteResponse(42L, 1, true);
        final QuestionResponse question = mock(QuestionResponse.class);
        when(voteService.add(eq(42L), any())).thenReturn(new VoteResult(response, question));

        mockMvc.perform(post("/api/questions/42/vote").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votedByMe").value(true));
    }

    @Test
    void add_CSRFなしなら403() throws Exception {
        mockMvc.perform(post("/api/questions/42/vote"))
                .andExpect(status().isForbidden());
    }

    @Test
    void remove_CSRFありなら200() throws Exception {
        final VoteResponse response = new VoteResponse(42L, 0, false);
        final QuestionResponse question = mock(QuestionResponse.class);
        when(voteService.remove(eq(42L), any())).thenReturn(new VoteResult(response, question));

        mockMvc.perform(delete("/api/questions/42/vote").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votedByMe").value(false));
    }

    @Test
    void remove_CSRFなしなら403() throws Exception {
        mockMvc.perform(delete("/api/questions/42/vote"))
                .andExpect(status().isForbidden());
    }

    /**
     * 本番 SecurityConfig は Import しない（OAuth2 依存を避ける）。
     * vote は permitAll + CSRF 有効だけ再現する。
     */
    static class VotePermitCsrfSecurityConfig {

        @Bean
        SecurityFilterChain votePermitCsrfFilterChain(HttpSecurity http) throws Exception {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(Customizer.withDefaults());
            return http.build();
        }
    }
}
