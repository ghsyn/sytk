package com.sytk.booking.domain;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ConcertEditor (
        String title,
        OffsetDateTime startAt,
        String venue,
        Integer runningTime,
        OffsetDateTime ticketOpenAt,
        OffsetDateTime ticketCloseAt
) {
}
