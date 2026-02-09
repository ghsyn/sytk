package com.sytk.booking.exception;

// TODO: GlobalExceptionHandler class 생성
// TODO: ErrorResponse record 생성
public class ConcertNotFoundException extends RuntimeException {

    private static final int STATUS_CODE = 404;
    private static final String MESSAGE = "존재하지 않는 공연입니다.";

    public ConcertNotFoundException() {
        super(MESSAGE);
    }

    public int getStatusCode() {
        return STATUS_CODE;
    }
}
