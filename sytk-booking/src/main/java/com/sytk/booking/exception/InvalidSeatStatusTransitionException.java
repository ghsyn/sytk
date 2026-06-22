package com.sytk.booking.exception;

import com.sytk.booking.domain.SeatStatus;

/**
 * 좌석 상태 변경이 불가능할 때 예외 발생
 */
public class InvalidSeatStatusTransitionException extends CommonException {
    public InvalidSeatStatusTransitionException(SeatStatus before, SeatStatus after) {
        super(
                ErrorCode.INVALID_SEAT_STATUS_TRANSITION,
                String.format("현재 좌석 상태 %s에서 %s(으)로 변경할 수 없습니다.", before.getDescription(), after.getDescription())
        );
    }
}
