package com.sytk.booking.response;

import com.sytk.booking.domain.Concert;
import lombok.Builder;

/**
 * 공연 등록 응답 DTO record
 */
@Builder
public record ConcertCreateResponse(
        Long id,
        String title
) {
    public static ConcertCreateResponse from(Concert concert) {
        return ConcertCreateResponse.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .build();
    }
}