package com.liveqa.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * エラーレスポンス用DTO
 * ErrorResponse
 * 
 * @param status
 * @param error
 * @param message
 * @param fields  バリデーション時の項目
 * @author mizumura
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fields) {

    public ErrorResponse {
        if (fields == null) {
            fields = Map.of();
        }
    }

    public static ErrorResponse of(
            int status,
            String error,
            String message) {
        return new ErrorResponse(status, error, message, Map.of());
    }

    public static ErrorResponse withField(
            int status,
            String error,
            String message,
            Map<String, String> fields) {

        if (fields == null) {
            fields = Map.of();
        }

        return new ErrorResponse(status, error, message, fields);
    }
}
