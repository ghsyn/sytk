package com.sytk.read.response;

import com.sytk.read.domain.Concert;

import java.time.OffsetDateTime;

/**
 * 공연 단건 조회 응답 DTO record
 */
public record ConcertDetailsResponse(
        Long id,
        String title,
        OffsetDateTime startAt,
        Integer runningTime,
        String venue,
        OffsetDateTime ticketOpenAt,
        OffsetDateTime ticketCloseAt
) {
    public static ConcertDetailsResponse from(Concert concert) {
        return new ConcertDetailsResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getStartAt(),
                concert.getRunningTime(),
                concert.getVenue(),
                concert.getTicketOpenAt(),
                concert.getTicketCloseAt()
        );
    }
}