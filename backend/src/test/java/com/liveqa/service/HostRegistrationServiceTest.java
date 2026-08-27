package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.liveqa.client.KeycloakAdminClient;
import com.liveqa.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class HostRegistrationServiceTest {

    @Mock
    KeycloakAdminClient keycloakAdminClient;

    @InjectMocks
    HostRegistrationService hostRegistrationService;

    @Test
    void register_正規化して発行する() {
        when(keycloakAdminClient.createHostUser("host@example.com")).thenReturn("user-1");

        hostRegistrationService.register("  Host@Example.com ");

        verify(keycloakAdminClient).createHostUser("host@example.com");
        verify(keycloakAdminClient).assignHostRole("user-1");
        verify(keycloakAdminClient).sendUpdatePasswordEmail("user-1");
    }

    @Test
    void register_既存なら409() {
        when(keycloakAdminClient.createHostUser("host@example.com"))
                .thenThrow(new BusinessException(
                        HttpStatus.CONFLICT, "Conflict", "このメールアドレスは既に登録されています。"));

        assertThatThrownBy(() -> hostRegistrationService.register("host@example.com"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }
}
