package com.liveqa.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.response.VoteResponse;
import com.liveqa.service.VoteService;

/**
 * VoteController の HTTP 面（配線スライス）。
 * Session の guestId は Controller、いいねロジックは Service（Mock）。
 * CSRF は {@link VoteControllerCsrfTest} で検証する。
 */
@WebMvcTest(
        controllers = VoteController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class VoteControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    VoteService voteService;

    @Test
    void add_200でSessionのguestIdをSourceに渡す() throws Exception {
        when(voteService.add(eq(42L), any())).thenReturn(new VoteResponse(42L, 1, true));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(post("/api/questions/42/vote").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(42))
                .andExpect(jsonPath("$.upvoteCount").value(1))
                .andExpect(jsonPath("$.votedByMe").value(true));

        final ArgumentCaptor<VoterKeySource> captor = ArgumentCaptor.forClass(VoterKeySource.class);
        verify(voteService).add(eq(42L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new VoterKeySource.Guest("abc"));
    }

    @Test
    void remove_200でSessionのguestIdをSourceに渡す() throws Exception {
        when(voteService.remove(eq(42L), any())).thenReturn(new VoteResponse(42L, 0, false));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(delete("/api/questions/42/vote").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(42))
                .andExpect(jsonPath("$.upvoteCount").value(0))
                .andExpect(jsonPath("$.votedByMe").value(false));

        final ArgumentCaptor<VoterKeySource> captor = ArgumentCaptor.forClass(VoterKeySource.class);
        verify(voteService).remove(eq(42L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new VoterKeySource.Guest("abc"));
    }
}
