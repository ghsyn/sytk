package com.sytk.booking.request;

import com.sytk.booking.domain.Reservation;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * 예매 생성 요청 DTO
 */
@Builder
public record ReservationCreateRequest(
        @NotNull(message = "유저 ID를 입력하세요.")
        Long userId,

        @NotNull(message = "좌석 ID를 입력하세요.")
        Long seatId
) {
    public Reservation toEntity() {
        return Reservation.builder()
                .userId(userId)
                .seatId(seatId)
                .build();
    }
}
