package com.liveqa.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liveqa.dto.response.ErrorResponse;

/**
 * 例外処理のクラス（401,403はConfigにて処理）
 * GlobalExceptionHandler
 * 
 * @author mizumura
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        /**
         * バリデーションエラーの処理
         * 
         * @param ex バリデーション時の例外
         * @return エラーレスポンス
         */
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
                                                LinkedHashMap::new));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.withField(
                                                400,
                                                "Bad Request",
                                                "入力内容に誤りがあります",
                                                fields));

        }

        /**
         * JSON が読めないときの処理（存在しない enum・壊れた JSON）
         * @Valid より前の Jackson で落ちる
         * @param ex デシリアライズ失敗
         * @return エラーレスポンス
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> unreadableHandler(HttpMessageNotReadableException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.of(
                                                400,
                                                "Bad Request",
                                                "リクエストの形式が正しくありません。"));
        }

        /**
         * 業務処理例外の処理
         * 
         * @param ex 業務処理例外
         * @return エラーレスポンス
         */
        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> businessHandler(BusinessException ex) {

                return ResponseEntity.status(ex.getStatus())
                                .body(ErrorResponse.of(
                                                ex.getStatus().value(),
                                                ex.getError(),
                                                ex.getMessage()));
        }

        /**
         * 認証失敗の処理
         * 
         * @param ex 認証例外
         * @return エラーレスポンス
         */
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> authenticationHandler(AuthenticationException ex) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of(
                                                401,
                                                "Unauthorized",
                                                "ログインが必要です"));
        }

        /**
         * 認可失敗の処理
         * 
         * @param ex 認可例外
         * @return エラーレスポンス
         */
        @ExceptionHandler({
                AccessDeniedException.class,
                AuthorizationDeniedException.class
        })
        public ResponseEntity<ErrorResponse> accessDeniedHandler(RuntimeException ex) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ErrorResponse.of(
                                                403,
                                                "Forbidden",
                                                "権限がありません"));
        }

        /**
         * 楽観ロック衝突の処理
         * 同じ行をロードして書いた操作同士（名称・開閉・削除）が同時のとき。
         * 削除が先に確定したあとの PATCH は行が無く 404（ここには来ない）
         * @param ex 版の不一致
         * @return エラーレスポンス
         */
        @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
        public ResponseEntity<ErrorResponse> optimisticLockHandler(
                ObjectOptimisticLockingFailureException ex
        ) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ErrorResponse.of(
                                                409,
                                                "Conflict",
                                                "他の操作と衝突しました。画面を更新して再度お試しください。"));
        }

        /**
         * 意図しない例外の処理
         * 
         * @param ex 意図しない例外
         * @return エラーレスポンス
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> ExceptionHandler(Exception ex) {
                log.error("未処理例外", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.of(
                                                500,
                                                "INTERNAL_SERVER_ERROR",
                                                "サーバーエラーが発生しました。"));
        }
}
