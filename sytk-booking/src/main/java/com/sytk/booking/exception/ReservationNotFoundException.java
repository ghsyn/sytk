package com.sytk.booking.exception;

public class ReservationNotFoundException extends CommonException {
    public ReservationNotFoundException() {
        super(ErrorCode.RESERVATION_NOT_FOUND);
    }
}
