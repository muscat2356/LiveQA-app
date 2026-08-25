package com.liveqa.dto.response;

import java.util.List;

import com.liveqa.domain.Poll;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;

/**
 * ポール作成・一覧・開閉の応答DTO
 * totalResponses / myOptionIds は F-11 まで 0 / 空
 * PollResponse
 * @author ichikura
 */
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
    
    /**
     * Entityから応答DTOへ変換するファクトリ
     * 得票は F-11 まで 0 固定
     * @param poll ポール
     * @return 応答DTO
     */
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
