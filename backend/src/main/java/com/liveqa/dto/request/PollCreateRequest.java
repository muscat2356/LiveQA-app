package com.liveqa.dto.request;

import java.util.List;

import com.liveqa.enums.PollType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PollCreateRequest(
    @NotBlank(message = "1～200文字で入力してください")
    @Size(min = 1, max = 200, message = "1～200文字で入力してください")
    String questionText,

    @NotNull(message = "type は必須です")
    PollType type,

    @NotNull(message = "選択肢は2～10個にしてください")
    @Size(min = 2, max = 10, message = "選択肢は2～10個にしてください")
    List<
        @NotBlank(message = "選択肢ラベルは1～100文字にしてください")
        @Size(max = 100, message = "選択肢ラベルは1～100文字にしてください")
        String
        > options
) {
    
}
