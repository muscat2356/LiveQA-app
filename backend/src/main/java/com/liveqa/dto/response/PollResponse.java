package com.liveqa.dto.response;

import java.util.List;
import java.util.Map;

import com.liveqa.domain.Poll;
import com.liveqa.enums.PollStatus;
import com.liveqa.enums.PollType;

/**
 * ポール作成・一覧・開閉・回答の応答DTO
 * 得票0は form(poll)（作成・開閉）。回答後は count / myOptionIds を載せる
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
     * 得票は0固定（作成・一覧・開閉）
     * @param poll ポール
     * @return 応答DTO
     */
    public static PollResponse form(Poll poll) {
        return form(poll, Map.of(), List.of());
    }

    /**
     * Entityから応答DTOへ変換するファクトリ
     * totalResponsesはcountの合計
     * @param poll ポール
     * @param countByOptionId 選択肢id→得票
     * @param myOptionIds 今のセッションが選んだ選択肢id
     * @return 応答DTO
     */
    public static PollResponse form(
        Poll poll,
        Map<Long, Integer> countByOptionId,
        List<Long> myOptionIds
    ) {
        final List<PollOptionResponse> options = 
            poll.getOptions().stream()
                .map(option -> {
                    final Long optionId = option.getId();
                    final int count = optionId == null
                     ? 0
                     : countByOptionId.getOrDefault(optionId, 0);
                    return PollOptionResponse.form(option, count);
                })
                .toList();

        final int totalResponses = options.stream()
                .mapToInt(PollOptionResponse::count)
                .sum();
        
        return new PollResponse(
            poll.getId(),
            poll.getEvent().getId(),
            poll.getQuestionText(),
            poll.getType(),
            poll.getStatus(),
            options,
            totalResponses,
            myOptionIds
        );
    }
}
