package com.sytk.booking.exception;

public class SeatNotFoundException extends CommonException {
    public SeatNotFoundException() {
        super(ErrorCode.SEAT_NOT_FOUND);
    }
}
