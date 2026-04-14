package com.sytk.booking.exception;

/**
 * 동일한 공연명 존재 시 예외 발생
 */
public class DuplicateConcertException extends CommonException {
    public DuplicateConcertException() {
        super(ErrorCode.DUPLICATE_CONCERT);
    }
}
