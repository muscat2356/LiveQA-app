package com.liveqa.dto.response;

import com.liveqa.domain.PollOption;

public record PollOptionResponse(
    Long id,
    String label,
    int count
) {
    
    public static PollOptionResponse form(PollOption option) {
        return new PollOptionResponse(
            option.getId(),
            option.getLabel(),
            0
        );
    }
}
