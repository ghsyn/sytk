package com.sytk.booking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class SeatStatusTest {
    @ParameterizedTest(name = "{0}에서 {1}로 전이 가능 여부: {2}")
    @CsvSource({
            "CLOSED, AVAILABLE, true",
            "CLOSED, OCCUPIED, false",
            "CLOSED, SOLD, false",

            "AVAILABLE, CLOSED, true",
            "AVAILABLE, OCCUPIED, true",
            "AVAILABLE, SOLD, false",

            "OCCUPIED, SOLD, true",
            "OCCUPIED, AVAILABLE, true",
            "OCCUPIED, CLOSED, false"
    })
    @DisplayName("좌석 상태 간의 전이 가능 비즈니스 규칙 검증")
    void statusTransitionRules(SeatStatus current, SeatStatus next, boolean expected) {
        // when
        boolean result = current.canChangeTo(next);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(SeatStatus.class)
    @DisplayName("판매 완료 상태에서는 어떤 상태로든 전이 불가")
    void soldCannotChangeToAny(SeatStatus next) {
        // given
        SeatStatus current = SeatStatus.SOLD;

        // when
        boolean result = current.canChangeTo(next);

        // then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SeatStatus.class)
    @DisplayName("동일한 상태로 전이 불가")
    void cannotChangeToSameStatus(SeatStatus status) {
        // when
        boolean result = status.canChangeTo(status);

        // then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "CLOSED, 미판매",
            "AVAILABLE, 예약 가능",
            "OCCUPIED, 선점",
            "SOLD, 판매 완료"
    })
    @DisplayName("좌석 상태의 설명이 올바르게 반환되는지 검증")
    void getDescription(SeatStatus status, String expect) {
        // when
        String actual = status.getDescription();

        // then
        assertThat(actual).isEqualTo(expect);
    }
}