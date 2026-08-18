package com.liveqa.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.liveqa.domain.VoterKey;
import com.liveqa.dto.response.GuestResponse;

@Service
public class GuestSessionService {
    public GuestResponse issueOrReuse(Optional<String> existing) {
        String guestId = existing
            .filter(s -> !s.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString());
        
        return new GuestResponse(
            guestId,
            VoterKey.guest(guestId).value()
        );
    }
}
