package com.liveqa.dto.response;

import com.liveqa.domain.PollOption;

/**
 * ポール選択肢1件の応答DTO
 * count は F-11 まで 0
 * PollOptionResponse
 * @author ichikura
 */
public record PollOptionResponse(
    Long id,
    String label,
    int count
) {
    
    /**
     * Entityから応答DTOへ変換するファクトリ
     * 得票は F-11 まで 0 固定
     * @param option 選択肢
     * @return 応答DTO
     */
    public static PollOptionResponse form(PollOption option) {
        return new PollOptionResponse(
            option.getId(),
            option.getLabel(),
            0
        );
    }
}
