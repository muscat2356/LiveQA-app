package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.liveqa.domain.User;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class VoterKeyResolverTest {

    @Mock
    UserService userService;

    @InjectMocks
    VoterKeyResolver voterKeyResolver;

    @Test
    void resolve_Hostならuキー() {
        final User user = User.hostOf("sub-1");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userService.requireByOidcSubject("sub-1")).thenReturn(user);

        final VoterKey key = voterKeyResolver.resolve(new VoterKeySource.Host("sub-1"));

        assertThat(key.value()).isEqualTo("u:1");
    }

    @Test
    void resolve_Guestならgキー() {
        final VoterKey key = voterKeyResolver.resolve(new VoterKeySource.Guest("abc"));

        assertThat(key.value()).isEqualTo("g:abc");
    }

    @Test
    void tryResolve_Missingならempty() {
        final Optional<VoterKey> key = voterKeyResolver.tryResolve(new VoterKeySource.Missing());

        assertThat(key).isEmpty();
    }

    @Test
    void require_Missingなら401() {
        assertThatThrownBy(() -> voterKeyResolver.require(new VoterKeySource.Missing()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    final BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(be.getMessage()).contains("匿名セッション");
                });
    }
}
