package com.liveqa.dto.response;

/**
 * いいね POST/DELETE のResponse
 * VoteResponse
 * @param questionId
 * @param upvoteCount
 * @param votedByMe
 * 
 * @author ichikura
 */
public record VoteResponse(
    Long questionId,
    int upvoteCount,
    boolean votedByMe
) {

    /**
     * いいね結果を応答DTOへ変換するファクトリ
     * upvoteCount / votedByMe はServiceが集める
     * @param questionId 質問id
     * @param upvoteCount 更新後のいいね数
     * @param votedByMe 今のセッションがいいね済みか
     * @return 応答DTO
     */
    public static VoteResponse form(
            Long questionId,
            int upvoteCount,
            boolean votedByMe) {
        return new VoteResponse(questionId, upvoteCount, votedByMe);
    }
}
