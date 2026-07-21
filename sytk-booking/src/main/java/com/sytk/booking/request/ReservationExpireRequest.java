package com.sytk.booking.request;

import lombok.Builder;

/**
 * 예매 만료 요청 DTO
 */
@Builder
public record ReservationExpireRequest(
        Long id
) {
}
