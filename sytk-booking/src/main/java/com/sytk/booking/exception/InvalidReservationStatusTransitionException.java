package com.sytk.booking.exception;

import com.sytk.booking.domain.ReservationStatus;

public class InvalidReservationStatusTransitionException extends CommonException {
    public InvalidReservationStatusTransitionException(ReservationStatus before, ReservationStatus after) {
        super(
                ErrorCode.INVALID_RESERVATION_STATUS_TRANSITION,
                String.format("현재 예매 상태 %s에서 %s(으)로 변경할 수 없습니다.", before.getDescription(), after.getDescription())
        );
    }
}
