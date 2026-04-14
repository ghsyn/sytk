package com.sytk.booking.exception;

/**
 * 부적절한 입력 요청 시 예외 발생
 */
public class InvalidRequestException extends CommonException {
    public InvalidRequestException() {
        super(ErrorCode.INVALID_REQUEST);
    }
}
