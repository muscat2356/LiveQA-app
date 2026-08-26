package com.liveqa.dto.response;

import com.liveqa.domain.PollOption;

/**
 * ポール選択肢1件の応答DTO
 * countは得票。未集計なら0
 * PollOptionResponse
 * @author ichikura
 */
public record PollOptionResponse(
    Long id,
    String label,
    int count
) {

    /**
     * 得票0の入り口（作成直後）
     * @param option 選択肢
     * @return 応答DTO（count=0）
     */
    public static PollOptionResponse form(PollOption option) {
        return form(option, 0);
    }

    /**
     * Entityから応答DTOへ変換するファクトリ
     * 得票はServiceが集めて渡す
     * @param option 選択肢
     * @param count この選択肢の得票
     * @return 応答DTO
     */
    public static PollOptionResponse form(PollOption option, int count) {
        return new PollOptionResponse(
            option.getId(),
            option.getLabel(),
            count
        );
    }
}
