package com.sytk.read.response;

import com.sytk.read.domain.Concert;

import java.time.OffsetDateTime;

/**
 * 공연 목록 조회 응답 DTO record
 */
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
