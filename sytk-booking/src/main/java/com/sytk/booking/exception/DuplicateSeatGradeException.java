package com.sytk.booking.exception;

/**
 * 한 공연에 동일한 좌석 등급명 요청 시 예외 발생
 */
public class DuplicateSeatGradeException extends CommonException{
    public DuplicateSeatGradeException() {
        super(ErrorCode.DUPLICATE_SEAT_GRADE);
    }
}