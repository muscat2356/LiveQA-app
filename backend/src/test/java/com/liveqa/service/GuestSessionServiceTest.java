package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.liveqa.dto.response.GuestResponse;

@ExtendWith(MockitoExtension.class)
class GuestSessionServiceTest {

    @InjectMocks
    GuestSessionService guestSessionService;

    @Test
    void issueOrReuse_空なら新規() {
        final GuestResponse response = guestSessionService.issueOrReuse(Optional.empty());

        assertThat(response.guestId()).isNotBlank();
        assertThat(response.voterKey()).isEqualTo("g:" + response.guestId());
    }

    @Test
    void issueOrReuse_既存があれば再利用() {
        final GuestResponse response = guestSessionService.issueOrReuse(Optional.of("fixed-id"));

        assertThat(response.guestId()).isEqualTo("fixed-id");
        assertThat(response.voterKey()).isEqualTo("g:fixed-id");
    }

    @Test
    void issueOrReuse_空白だけなら新規() {
        final GuestResponse response = guestSessionService.issueOrReuse(Optional.of("  "));

        assertThat(response.guestId()).isNotBlank();
        assertThat(response.guestId()).isNotEqualTo("  ");
        assertThat(response.voterKey()).isEqualTo("g:" + response.guestId());
    }
}
