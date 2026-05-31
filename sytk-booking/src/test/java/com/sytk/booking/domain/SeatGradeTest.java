package com.sytk.booking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeatGradeTest {

    @Test
    @DisplayName("[성공케이스] 지정된 totalSeatCount만큼 1번부터 시작하는 CLOSED 상태의 좌석 생성")
    void createSeats_success() {
        // given
        SeatGrade seatGrade = SeatGrade.builder()
                .name("VIP")
                .price(BigDecimal.valueOf(150000))
                .totalSeatCount(5)
                .build();

        // when
        List<Seat> seatList = seatGrade.createSeats();

        // then
        assertThat(seatList).hasSize(seatGrade.getTotalSeatCount());
        assertThat(seatList.get(0).getNumber()).isEqualTo(1);
        assertThat(seatList.get(0).getStatus()).isEqualTo(SeatStatus.CLOSED);
        assertThat(seatList.get(seatList.size() - 1).getNumber()).isEqualTo(seatGrade.getTotalSeatCount());
    }

    @Test
    @DisplayName("[성공케이스 - 경계값 테스트] totalSeatCount가 0일 경우 빈 좌석 리스트 반환")
    void createSeats_zero_count_success() {
        // given
        SeatGrade seatGrade = SeatGrade.builder()
                    .name("VIP")
                .price(BigDecimal.valueOf(150000))
                .totalSeatCount(0)
                .build();

        // when
        List<Seat> seatList = seatGrade.createSeats();

        // then
        assertThat(seatList).isEmpty();
    }
}