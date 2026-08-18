package com.liveqa.dto.response;

import com.liveqa.enums.EventStatus;

public record EventJoinResponse(
    Long eventId,
    String title,
    EventStatus status
) {

}
