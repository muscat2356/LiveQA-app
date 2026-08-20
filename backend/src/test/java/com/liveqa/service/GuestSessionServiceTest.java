package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.liveqa.domain.VoterKeySource;
import com.liveqa.dto.response.GuestResponse;
import com.liveqa.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class GuestSessionServiceTest {

    @InjectMocks
    GuestSessionService guestSessionService;

    @Test
    void issueOrReuse_Missingなら新規() {
        final GuestResponse response =
                guestSessionService.issueOrReuse(new VoterKeySource.Missing());

        assertThat(response.guestId()).isNotBlank();
        assertThat(response.voterKey()).isEqualTo("g:" + response.guestId());
    }

    @Test
    void issueOrReuse_Guestなら再利用() {
        final GuestResponse response =
                guestSessionService.issueOrReuse(new VoterKeySource.Guest("fixed-id"));

        assertThat(response.guestId()).isEqualTo("fixed-id");
        assertThat(response.voterKey()).isEqualTo("g:fixed-id");
    }

    @Test
    void issueOrReuse_Hostなら400() {
        assertThatThrownBy(() ->
                guestSessionService.issueOrReuse(new VoterKeySource.Host("sub-1")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessage()).contains("匿名セッション");
                });
    }
}
