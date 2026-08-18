package com.liveqa.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.liveqa.dto.response.EventJoinResponse;
import com.liveqa.enums.EventStatus;
import com.liveqa.service.EventService;

/**
 * JoinController の HTTP 面（公開 GET）。
 * 本番は permitAll。スライスではフィルタを外して JSON 配線だけ見る。
 */
@WebMvcTest(
        controllers = JoinController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class JoinControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EventService eventService;

    @Test
    void findByJoinCode_200() throws Exception {
        when(eventService.findByJoinCode("Ab12Cd34"))
                .thenReturn(new EventJoinResponse(10L, "第一回", EventStatus.OPEN));

        mockMvc.perform(get("/api/join/Ab12Cd34"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(10))
                .andExpect(jsonPath("$.title").value("第一回"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }
}
