package com.liveqa.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * エラー応答DTO（{ status, error, message }。400は任意でfields）
 * ErrorResponse
 * @author mizumura
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fields) {

    /**
     * fieldsがnullなら空Mapへ正規化する
     */
    public ErrorResponse {
        if (fields == null) {
            fields = Map.of();
        }
    }

    /**
     * fields無しのエラー応答を組み立てるファクトリ
     * @param status HTTPステータス
     * @param error エラー種別
     * @param message 表示用メッセージ
     * @return fieldsが空のErrorResponse
     */
    public static ErrorResponse of(
            int status,
            String error,
            String message) {
        return new ErrorResponse(status, error, message, Map.of());
    }

    /**
     * 項目エラー付きのエラー応答を組み立てるファクトリ
     * @param status HTTPステータス
     * @param error エラー種別
     * @param message 表示用メッセージ
     * @param fields 項目名→メッセージ（nullなら空Map）
     * @return fields付きのErrorResponse
     */
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
