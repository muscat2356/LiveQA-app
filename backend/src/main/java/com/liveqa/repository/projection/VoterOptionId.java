package com.liveqa.repository.projection;

/**
 * 今のセッションが選んだ選択肢（一覧の myOptionIds 用）
 * Object[]で受けると型が消えるためrecordで受ける
 * VoterOptionId
 * @author ichikura
 */
public record VoterOptionId(Long pollId, Long optionId) {
}
