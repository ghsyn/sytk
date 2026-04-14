package com.sytk.booking.request;

import com.sytk.booking.request.validation.ValidTicketTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

import java.time.OffsetDateTime;

/**
 * 공연 수정 dto
 */
@Builder
@ValidTicketTime
public record ConcertEditRequest(
        @NotBlank(message = "제목을 입력하세요.")
        @Length(min = 1, max = 255, message = "제목은 1 ~ 255 글자로 입력해주세요.")
        String title,
        @NotNull(message = "시작시간을 입력하세요.")
        OffsetDateTime startAt,
        @NotBlank(message = "장소를 입력하세요.")
        @Length(min = 10, max = 255, message = "장소는 10 ~ 255 글자로 입력해주세요.")
        String venue,
        Integer runningTime,
        OffsetDateTime ticketOpenAt,
        OffsetDateTime ticketCloseAt
) {

}
