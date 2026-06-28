package com.sytk.booking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTest {

    @ParameterizedTest
    @EnumSource(ReservationStatus.class)
    @DisplayName("대기 상태에서는 동일 상태를 제외한 어떤 상태로든 전이 가능")
    void reservingCanChangeToAnyExceptSelf(ReservationStatus next) {
        // given
        ReservationStatus current = ReservationStatus.RESERVING;

        // when
        boolean result = current.canChangeTo(next);

        // then
        if (next == ReservationStatus.RESERVING) {
            assertThat(result).isFalse();
        } else {
            assertThat(result).isTrue();
        }
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMED", "CANCELED", "EXPIRED"})
    @DisplayName("확정, 취소, 만료 상태에서는 어떤 상태로든 전이 불가")
    void terminalStatusCannotChangeToAny(ReservationStatus terminalStatus) {
        for (ReservationStatus next : ReservationStatus.values()) {
            // when
            boolean result = terminalStatus.canChangeTo(next);

            assertThat(result).isFalse();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "RESERVING, 대기",
            "CONFIRMED, 확정",
            "CANCELED, 취소",
            "EXPIRED, 만료"
    })
    @DisplayName("예매 상태의 설명이 올바르게 반환되는지 검증")
    void getDescription(ReservationStatus status, String expected) {
        // when
        String actual = status.getDescription();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
