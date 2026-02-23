package com.sytk.booking.request;

import com.sytk.booking.domain.Concert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.OffsetDateTime;

/**
 * 공연 등록 record
 */
@Builder
public record ConcertCreateRequest(
        @NotBlank(message = "제목을 입력하세요.")
        String title,
        @NotNull(message = "시작시간을 입력하세요.")
        OffsetDateTime startAt,
        @NotBlank(message = "장소를 입력하세요.")
        String location,
        Integer runningTime,
        OffsetDateTime ticketOpenAt,
        OffsetDateTime ticketCloseAt
) {
        public Concert toEntity() {
                return Concert.builder()
                        .title(title)
                        .startAt(startAt)
                        .location(location)
                        .runningTime(runningTime)
                        .ticketOpenAt(ticketOpenAt)
                        .ticketCloseAt(ticketCloseAt)
                        .build();
        }
}
