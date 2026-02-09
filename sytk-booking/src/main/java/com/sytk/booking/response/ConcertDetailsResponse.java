package com.sytk.booking.response;

import com.sytk.booking.domain.Concert;

import java.time.OffsetDateTime;

/**
 * 공연 단건 조회 응답 DTO record
 */
public record ConcertDetailsResponse(
        Long id,
        String title,
        OffsetDateTime startAt,
        Integer runningTime,
        String location,
        OffsetDateTime ticketOpenAt,
        OffsetDateTime ticketCloseAt
) {
    public static ConcertDetailsResponse from(Concert concert) {
        return new ConcertDetailsResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getStartAt(),
                concert.getRunningTime(),
                concert.getLocation(),
                concert.getTicketOpenAt(),
                concert.getTicketCloseAt()
        );
    }
}