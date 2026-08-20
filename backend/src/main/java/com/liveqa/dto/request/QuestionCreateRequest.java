package com.liveqa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 質問投稿POSTの入力DTO
 * QuestionCreateRequest
 * @author ichikura
 */
public record QuestionCreateRequest(
    @NotBlank(message = "1～500文字で入力してください")
    @Size(min = 1, max = 500, message = "1～500文字で入力してください")
    String body,

    @Size(max = 50, message = "1～50文字で入力してください")
    String nickname
) {

}
