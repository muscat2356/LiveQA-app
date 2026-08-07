package com.liveqa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.liveqa.domain.User;
import com.liveqa.dto.response.MeResponse;
import com.liveqa.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository repository;

    @InjectMocks
    UserService userService;

    @Test
    void provisionHost_新規の場合はsave() {
        when(repository.findByOidcSubject("sub-1"))
                .thenReturn(Optional.empty());
        when(repository.save(any(User.class)))
                .thenAnswer(inv -> {
                    final User u = inv.getArgument(0);
                    ReflectionTestUtils.setField(u, "id", 1L);
                    return u;
                });

        final User result = userService.provisionHost("sub-1");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOidcSubject()).isEqualTo("sub-1");
        assertThat(result.getRole()).isEqualTo("HOST");
        verify(repository).save(any(User.class));
    }

    @Test
    void provisionHost_既存の場合はsaveしない() {
        final User existing = User.hostOf("sub-1");
        ReflectionTestUtils.setField(existing, "id", 9L);
        when(repository.findByOidcSubject("sub-1"))
                .thenReturn(Optional.of(existing));

        assertThat(userService.provisionHost("sub-1").getId()).isEqualTo(9L);
        verify(repository, never()).save(any());
    }

    @Test
    void requireByOidcSubject_存在すれば返す() {
        final User existing = User.hostOf("sub-1");
        ReflectionTestUtils.setField(existing, "id", 3L);
        when(repository.findByOidcSubject("sub-1"))
                .thenReturn(Optional.of(existing));

        final User result = userService.requireByOidcSubject("sub-1");

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getOidcSubject()).isEqualTo("sub-1");
    }

    @Test
    void requireByOidcSubject_無ければ例外() {
        when(repository.findByOidcSubject("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.requireByOidcSubject("missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JIT未実行");
    }

    @Test
    void toMeResponse_fullNameがあれば優先() {
        final User local = User.hostOf("sub-1");
        ReflectionTestUtils.setField(local, "id", 1L);
        final OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getFullName()).thenReturn("山田太郎");
        when(oidcUser.getEmail()).thenReturn("taro@example.com");

        final MeResponse response = userService.toMeResponse(local, oidcUser);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.displayName()).isEqualTo("山田太郎");
        assertThat(response.email()).isEqualTo("taro@example.com");
        assertThat(response.role()).isEqualTo("HOST");
    }

    @Test
    void toMeResponse_fullNameが無ければpreferredUsername() {
        final User local = User.hostOf("sub-1");
        ReflectionTestUtils.setField(local, "id", 2L);
        final OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getFullName()).thenReturn(null);
        when(oidcUser.getPreferredUsername()).thenReturn("host1");
        when(oidcUser.getEmail()).thenReturn("host1@example.com");

        final MeResponse response = userService.toMeResponse(local, oidcUser);

        assertThat(response.displayName()).isEqualTo("host1");
        assertThat(response.id()).isEqualTo(2L);
    }

    @Test
    void currentHost_復元してMeResponseを組み立てる() {
        final User local = User.hostOf("sub-1");
        ReflectionTestUtils.setField(local, "id", 7L);
        when(repository.findByOidcSubject("sub-1"))
                .thenReturn(Optional.of(local));

        final OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getSubject()).thenReturn("sub-1");
        when(oidcUser.getFullName()).thenReturn("主催者");
        when(oidcUser.getEmail()).thenReturn("host@example.com");

        final MeResponse response = userService.currentHost(oidcUser);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.displayName()).isEqualTo("主催者");
        assertThat(response.email()).isEqualTo("host@example.com");
        assertThat(response.role()).isEqualTo("HOST");
    }
}
