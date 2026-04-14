package com.sytk.booking.exception;

/**
 * 접근 공연 대상 존재하지 않을 시 예외 발생
 */
public class ConcertNotFoundException extends CommonException {

    public ConcertNotFoundException() {
        super(ErrorCode.CONCERT_NOT_FOUND);
    }
}
