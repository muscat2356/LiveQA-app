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

}
