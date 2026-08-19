package com.liveqa.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.liveqa.domain.User;
import com.liveqa.domain.VoterKey;
import com.liveqa.domain.VoterKeySource;
import com.liveqa.exception.BusinessException;

@Service
public class VoterKeyResolver {

    private final UserService userService;

    public VoterKeyResolver(UserService userService) {
        this.userService = userService;
    }

    public Optional<VoterKey> tryResolve(VoterKeySource source) {
        return switch (source) {
            case VoterKeySource.Host(String subject) -> {
                final User user = userService.requireByOidcSubject(subject);
                yield Optional.of(VoterKey.host(user.getId()));
            }
            case VoterKeySource.Guest(String guestId) -> Optional.of(VoterKey.guest(guestId));
            case VoterKeySource.Missing() -> Optional.empty();
        };
    }

    public VoterKey resolve(VoterKeySource source){
        return tryResolve(source).orElseThrow(this::unauthorized);
    }

    public void require(VoterKeySource source) {
        resolve(source);
    }

    private BusinessException unauthorized () {
        return new BusinessException(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "匿名セッションが未確立です。"
        );
    }

}
