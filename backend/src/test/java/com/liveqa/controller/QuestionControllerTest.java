package com.liveqa.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.request.QuestionCreateRequest;
import com.liveqa.dto.response.QuestionResponse;
import com.liveqa.enums.QuestionStatus;
import com.liveqa.service.QuestionService;

/**
 * QuestionController の HTTP 面（配線スライス）。
 * Session の guestId は Controller、投稿ロジックは Service（Mock）。
 */
@WebMvcTest(
        controllers = QuestionController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class QuestionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    QuestionService questionService;

    @Test
    void create_201でSessionのguestIdをSourceに渡す() throws Exception {
        when(questionService.create(eq(10L), any(), any()))
                .thenReturn(new QuestionResponse(
                        42L,
                        10L,
                        "資料は公開されますか？",
                        "花子",
                        0,
                        QuestionStatus.OPEN,
                        false,
                        Instant.parse("2026-08-19T00:00:00Z")));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(post("/api/events/10/questions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"資料は公開されますか？\",\"nickname\":\"花子\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.eventId").value(10))
                .andExpect(jsonPath("$.displayName").value("花子"))
                .andExpect(jsonPath("$.votedByMe").value(false));

        final ArgumentCaptor<VoterKeySource> captor = ArgumentCaptor.forClass(VoterKeySource.class);
        verify(questionService).create(eq(10L), any(QuestionCreateRequest.class), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new VoterKeySource.Guest("abc"));
    }

    @Test
    void findByEventId_200でGuestを渡す() throws Exception {
        when(questionService.findByEventId(eq(10L), any()))
                .thenReturn(List.of(new QuestionResponse(
                        42L,
                        10L,
                        "資料は公開されますか？",
                        "花子",
                        0,
                        QuestionStatus.OPEN,
                        false,
                        Instant.parse("2026-08-19T00:00:00Z"))));

        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(GuestSession.ATTR_GUEST_ID, "abc");

        mockMvc.perform(get("/api/events/10/questions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].votedByMe").value(false));

        final ArgumentCaptor<VoterKeySource> captor = ArgumentCaptor.forClass(VoterKeySource.class);
        verify(questionService).findByEventId(eq(10L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new VoterKeySource.Guest("abc"));
    }
}
