package com.sytk.booking.exception;

/**
 * 변경 사항 없을 시 예외 발생
 */
public class NotChangedException extends CommonException {

    public NotChangedException() {
        super(ErrorCode.NOT_CHANGED);
    }
}
