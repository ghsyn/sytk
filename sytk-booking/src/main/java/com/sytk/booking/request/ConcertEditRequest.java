package com.sytk.booking.request;

import com.sytk.booking.request.validation.ValidTicketTime;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

import java.time.OffsetDateTime;

/**
 * 공연 수정 dto
 */
@Builder
@ValidTicketTime
public record ConcertEditRequest(
        @Pattern(regexp = ".*\\S.*")
        @Length(min = 1, max = 255, message = "제목은 1 ~ 255 글자로 입력해주세요.")
        String title,
        OffsetDateTime startAt,
        @Pattern(regexp = ".*\\S.*")
        @Length(min = 4, max = 255, message = "장소는 4 ~ 255 글자로 입력해주세요.")
        String venue,
        Integer runningTime,
        OffsetDateTime ticketOpenAt,
        OffsetDateTime ticketCloseAt
) {

}
