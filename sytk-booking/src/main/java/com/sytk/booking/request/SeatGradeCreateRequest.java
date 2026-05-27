package com.sytk.booking.request;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.domain.SeatGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

/**
 * 좌석 등급 등록 dto
 */
@Builder
public record SeatGradeCreateRequest(
        @NotBlank(message = "등급명을 입력하세요.")
        @Length(min = 1, max = 255, message = "등급명은 1 ~ 255 글자로 입력해주세요.")
        String name,
        @NotNull(message = "가격을 입력하세요.")
        BigDecimal price,
        @NotNull(message = "해당 등급의 총 좌석 수를 입력하세요.")
        Integer totalSeatCount
) {
    public SeatGrade toEntity(Concert concert) {
        return SeatGrade.builder()
                .name(name)
                .price(price)
                .totalSeatCount(totalSeatCount)
                .concert(concert)
                .build();
    }
}
