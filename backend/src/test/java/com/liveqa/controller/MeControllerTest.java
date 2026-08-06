package com.liveqa.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.dto.response.MeResponse;
import com.liveqa.service.UserService;

/**
 * MeController の HTTP 面を検証する。
 * 本番 SecurityConfig は Import せず、@PreAuthorize だけ有効化して HOST / 非HOST / 未認証を分ける。
 * OAuth2Client 自動設定は issuer 解決で Keycloak に繋がるため除外する。
 */
@WebMvcTest(
        controllers = MeController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import(MeControllerTest.MethodSecurityTestConfig.class)
class MeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    void me_HOSTなら200とMeResponse() throws Exception {
        when(userService.currentHost(any(OidcUser.class)))
                .thenReturn(new MeResponse(1L, "山田太郎", "taro@example.com", "HOST"));

        mockMvc.perform(get("/api/me").with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_HOST"))
                        .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.displayName").value("山田太郎"))
                .andExpect(jsonPath("$.email").value("taro@example.com"))
                .andExpect(jsonPath("$.role").value("HOST"));
    }

    @Test
    void me_HOST以外なら403() throws Exception {
        mockMvc.perform(get("/api/me").with(oidcLogin()
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        .idToken(id -> id.subject("sub-1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_未認証なら401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    /** @WebMvcTest では本番 SecurityConfig が載らないため、メソッド認可だけ足す */
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
