package com.sytk.booking.request;

import com.sytk.booking.domain.Reservation;
import lombok.Builder;

/**
 * 예매 생성 요청 DTO
 */
@Builder
public record ReservationCreateRequest(
        Long userId,
        Long seatId
) {
    public Reservation toEntity() {
        return Reservation.builder()
                .userId(userId)
                .seatId(seatId)
                .build();
    }
}
