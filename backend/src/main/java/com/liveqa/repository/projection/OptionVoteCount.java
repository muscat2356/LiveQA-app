package com.liveqa.repository.projection;

/**
 * 選択肢1件の得票（GROUP BYの受け皿）
 * Object[]で受けると型が消えるためrecordで受ける
 * OptionVoteCount
 * @author ichikura
 */
public record OptionVoteCount(Long optionId, long count) {
}
