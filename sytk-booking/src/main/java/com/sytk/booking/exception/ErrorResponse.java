package com.sytk.booking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;

/**
 * 전역 에러 응답 클래스
 *
 * {
 *     "status" : 400,
 *     "message" : "잘못된 입력값입니다.",
 *     "timestamp" : "2026-03-16T22:00:15.740015",
 *     "validation" : {
 *         "title" : "제목을 입력하세요."
 *     }
 * }
 */
@Builder
public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)     // 검증 오류 없을 시 JSON 결과에서 validation 항목 제거
        Map<String, String> validation
) {
    /**
     * 일반 에러용
     */
    public static ErrorResponse from(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .timestamp(now())
                .validation(Map.of())
                .build();
    }

    /**
     * 검증 에러용(validationExceptionHandler)
     */
    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        Map<String, String> validation = new HashMap<>();

        bindingResult.getFieldErrors().forEach(fieldError ->
                validation.put(fieldError.getField(), fieldError.getDefaultMessage())
        );

        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .timestamp(now())
                .validation(validation)
                .build();
    }
}
