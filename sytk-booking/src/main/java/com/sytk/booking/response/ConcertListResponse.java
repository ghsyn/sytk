package com.sytk.booking.response;

import com.sytk.booking.domain.Concert;

import java.time.OffsetDateTime;

public record ConcertListResponse(
        Long id,
        String title,
        OffsetDateTime startAt,
        Integer runningTime,
        String location
) {
    public static ConcertListResponse from(Concert concert) {
        return new ConcertListResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getStartAt(),
                concert.getRunningTime(),
                concert.getLocation()
        );
    }
}
