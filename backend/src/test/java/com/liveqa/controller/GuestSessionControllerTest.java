package com.liveqa.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.dto.response.GuestResponse;
import com.liveqa.service.GuestSessionService;

/**
 * GuestSessionController の HTTP 面（配線スライス）。
 * Session の get/set は Controller、発行ロジックは Service（Mock）。
 * CSRF は {@link GuestSessionControllerCsrfTest} で検証する。
 */
@WebMvcTest(
        controllers = GuestSessionController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class GuestSessionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GuestSessionService guestSessionService;

    @Test
    void createGuestSession_属性なしならemptyを渡して200() throws Exception {
        when(guestSessionService.issueOrReuse(any()))
                .thenReturn(new GuestResponse("new-id", "g:new-id"));

        final MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/guest").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestId").value("new-id"))
                .andExpect(jsonPath("$.voterKey").value("g:new-id"));

        final ArgumentCaptor<Optional<String>> captor = ArgumentCaptor.forClass(Optional.class);
        verify(guestSessionService).issueOrReuse(captor.capture());
        assertThat(captor.getValue()).isEmpty();
        assertThat(session.getAttribute(GuestSession.ATTR_GUEST_ID)).isEqualTo("new-id");
    }

    @Test
    void createGuestSession_既存属性があればOptionalで渡す() throws Exception {
        when(guestSessionService.issueOrReuse(any()))
                .thenReturn(new GuestResponse("fixed-id", "g:fixed-id"));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "fixed-id");

        mockMvc.perform(post("/api/auth/guest").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestId").value("fixed-id"))
                .andExpect(jsonPath("$.voterKey").value("g:fixed-id"));

        final ArgumentCaptor<Optional<String>> captor = ArgumentCaptor.forClass(Optional.class);
        verify(guestSessionService).issueOrReuse(captor.capture());
        assertThat(captor.getValue()).contains("fixed-id");
        assertThat(session.getAttribute(GuestSession.ATTR_GUEST_ID)).isEqualTo("fixed-id");
    }
}
