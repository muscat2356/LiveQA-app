package com.liveqa.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.liveqa.dto.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationHandler(MethodArgumentNotValidException ex) {

        final Map<String, String> fields = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Objects.requireNonNullElse(
                                fieldError.getDefaultMessage(),
                                ""),
                        (first, second) -> first,
                        LinkedHashMap::new
                    ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withField(
                        400,
                        "Bad Request",
                        "入力内容に誤りがあります",
                        fields));

    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> businessHandler(BusinessException ex) {

        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(
                        ex.getStatus().value(),
                        ex.getError(),
                        ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> ExceptionHandler(Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        500,
                        "INTERNAL_SERVER_ERROR",
                        "サーバーエラーが発生しました。"));
    }
}
