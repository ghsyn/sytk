package com.sytk.booking.exception;

/**
 * 이미 예매된 좌석일 경우 발생하는 예외
 */
public class SeatAlreadyOccupiedException extends CommonException {
    public SeatAlreadyOccupiedException() {
        super(ErrorCode.SEAT_ALREADY_OCCUPIED);
    }
}
