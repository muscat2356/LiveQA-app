package com.liveqa.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.exception.BusinessException;
import com.liveqa.exception.GlobalExceptionHandler;
import com.liveqa.service.HostRegistrationService;

/**
 * HostRegistrationController の HTTP 面（配線スライス）。
 * CSRF は {@link HostRegistrationControllerCsrfTest} で検証する。
 */
@WebMvcTest(
        controllers = HostRegistrationController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HostRegistrationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HostRegistrationService hostRegistrationService;

    @Test
    void register_形式OKなら202() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"host@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(hostRegistrationService).register("host@example.com");
    }

    @Test
    void register_形式不正なら400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void register_既存なら409() throws Exception {
        doThrow(new BusinessException(
                HttpStatus.CONFLICT, "Conflict", "このメールアドレスは既に登録されています。"))
                .when(hostRegistrationService)
                .register("host@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"host@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
