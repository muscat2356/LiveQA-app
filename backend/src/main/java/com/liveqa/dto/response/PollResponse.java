package com.liveqa.dto.response;

import java.util.List;

import com.liveqa.domain.Poll;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;

public record PollResponse(
    Long id,
    Long eventId,
    String questionText,
    PollType type,
    PollStatus status,
    List<PollOptionResponse> options,
    int totalResponses,
    List<Long> myOptionIds
) {
    
    public static PollResponse form(Poll poll) {
        final List<PollOptionResponse> options = poll.getOptions()
                .stream()
                .map(PollOptionResponse::form)
                .toList();
        
        return new PollResponse(
            poll.getId(),
            poll.getEvent().getId(),
            poll.getQuestionText(),
            poll.getType(),
            poll.getStatus(),
            options,
            0,
            List.of()
        );
    }
}
