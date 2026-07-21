package com.sytk.booking.request;

import lombok.Builder;

/**
 * 예매 확정 요청 DTO
 */
@Builder
public record ReservationConfirmRequest(
        Long id
) {
}
