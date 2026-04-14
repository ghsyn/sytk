package com.sytk.booking.response;

import lombok.Builder;

/**
 * 공연 수정 응답 DTO record
 */
@Builder
public record ConcertEditResponse(
        Long id,
        String title
) {
}
