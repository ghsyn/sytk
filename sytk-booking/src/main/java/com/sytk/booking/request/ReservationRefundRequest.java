package com.sytk.booking.request;

/**
 * 예매 환불 요청 DTO
 */

import lombok.Builder;

@Builder
public record ReservationRefundRequest(
        Long id
) {
}
