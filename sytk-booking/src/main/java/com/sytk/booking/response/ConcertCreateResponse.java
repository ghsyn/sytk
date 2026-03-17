package com.sytk.booking.response;

import com.sytk.booking.domain.Concert;

/**
 * 공연 등록 응답 DTO record
 */
public record ConcertCreateResponse(
        Long id,
        String title
) {
    public static ConcertCreateResponse from(Concert concert) {
        return new ConcertCreateResponse(
                concert.getId(),
                concert.getTitle()
        );
    }
}