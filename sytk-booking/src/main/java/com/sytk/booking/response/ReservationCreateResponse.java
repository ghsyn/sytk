package com.sytk.booking.response;

import com.sytk.booking.domain.Reservation;
import com.sytk.booking.domain.ReservationStatus;
import lombok.Builder;

/**
 * 예매 생성 응답 DTO
 */
@Builder
public record ReservationCreateResponse(
        Long id,
        ReservationStatus status
) {
    public static ReservationCreateResponse from(Reservation reservation) {
        return ReservationCreateResponse.builder()
                .id(reservation.getId())
                .status(reservation.getStatus())
                .build();
    }
}
