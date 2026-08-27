package com.liveqa.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * HOST 発行 POST の入力DTO。パスワードは持たない
 * HostRegisterRequest
 * @author ichikura
 */
public record HostRegisterRequest(
        @NotBlank(message = "メールアドレスの形式が正しくありません")
        @Email(message = "メールアドレスの形式が正しくありません")
        @Size(max = 254, message = "メールアドレスの形式が正しくありません")
        String email) {
}
