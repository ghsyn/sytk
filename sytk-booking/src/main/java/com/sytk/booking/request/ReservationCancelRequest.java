package com.sytk.booking.request;

import lombok.Builder;

/**
 * 예매 취소 요청 DTO
 */
@Builder
public record ReservationCancelRequest(
        Long id
) {
}
