package com.liveqa.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.service.HostRegistrationService;

/**
 * HOST 発行 POST の CSRF（Filter 層）。
 * 発行口は ignore。あり／なしどちらも 202。他 path の CSRF は見ない。
 */
@WebMvcTest(
        controllers = HostRegistrationController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import(HostRegistrationControllerCsrfTest.RegisterPermitCsrfSecurityConfig.class)
class HostRegistrationControllerCsrfTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HostRegistrationService hostRegistrationService;

    @Test
    void register_CSRFありなら202() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"host@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void register_CSRFなしでも202() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"host@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    /**
     * 本番 SecurityConfig は Import しない（OAuth2 依存を避ける）。
     * register の ignore だけ本番と同じ matcher で再現する。
     */
    static class RegisterPermitCsrfSecurityConfig {

        @Bean
        SecurityFilterChain registerPermitCsrfFilterChain(HttpSecurity http) throws Exception {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/register"));
            return http.build();
        }
    }
}
