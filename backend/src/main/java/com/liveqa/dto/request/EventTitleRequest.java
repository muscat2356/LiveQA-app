package com.liveqa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * イベント名更新（PATCH /api/events/{id}）の入力
 */
public record EventTitleRequest(
    @Size(min = 1, max = 100, message = "1～100文字で入力してください")
    @NotBlank(message = "1～100文字で入力してください")
    String title
) {

}
