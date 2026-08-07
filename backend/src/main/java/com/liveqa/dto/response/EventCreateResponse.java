package com.liveqa.dto.response;


import java.time.Instant;

import com.liveqa.enums.EventStatus;

public record EventCreateResponse(
        Long id,
        String title,
        String joinCode,
        String joinUrl,
        EventStatus status,
        Instant createdAt) {

}
