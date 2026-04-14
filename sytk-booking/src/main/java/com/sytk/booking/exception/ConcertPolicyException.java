package com.sytk.booking.exception;

/**
 * 예매 내역 존재하는 공연 삭제 시도 시 예외 발생
 */
public class ConcertPolicyException extends CommonException {
    public ConcertPolicyException() {super(ErrorCode.RESERVED_CONCERT);}
}
