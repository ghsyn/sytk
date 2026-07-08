package com.sytk.booking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTest {

    /**
     * 대기(RESERVING)
     */
    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CONFIRMED", "CANCELED", "EXPIRED"})
    @DisplayName("대기(RESERVING) 상태에서는 확정/취소/만료로 전이 가능")
    void reservingCanChangeToAllowedStatuses(ReservationStatus next) {
        boolean result = ReservationStatus.RESERVING.canChangeTo(next);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"RESERVING", "REFUNDED"})
    @DisplayName("대기(RESERVING) 상태에서는 자기 자신과 환불로는 전이 불가")
    void reservingCannotChangeToSelfOrRefunded(ReservationStatus next) {
        boolean result = ReservationStatus.RESERVING.canChangeTo(next);
        assertThat(result).isFalse();
    }

    /**
     * 확정(CONFIRMED)
     */
    @ParameterizedTest
    @EnumSource(ReservationStatus.class)
    @DisplayName("확정(CONFIRMED) 상태 에서는 환불(REFUNDED) 상태로만 전이 가능")
    void confirmedCanChangeToRefundedOnly(ReservationStatus next) {
        // given
        ReservationStatus current = ReservationStatus.CONFIRMED;

        // when
        boolean result = current.canChangeTo(next);

        // then
        if (next == ReservationStatus.REFUNDED) {
            assertThat(result).isTrue();
        } else {
            assertThat(result).isFalse();
        }
    }

    /**
     * 취소(CANCELED), 만료 상태에서는(EXPIRED), 환불(REFUNDED)
     */
    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CANCELED", "EXPIRED", "REFUNDED"})
    @DisplayName("취소(CANCELED), 만료 상태에서는(EXPIRED), 환불(REFUNDED) 어떤 상태로든 전이 불가")
    void terminalStatusCannotChangeToAny(ReservationStatus terminalStatus) {
        for (ReservationStatus next : ReservationStatus.values()) {
            // when
            boolean result = terminalStatus.canChangeTo(next);

            assertThat(result).isFalse();
        }
    }

    /**
     * Description
     */
    @ParameterizedTest
    @CsvSource({
            "RESERVING, 대기",
            "CONFIRMED, 확정",
            "CANCELED, 취소",
            "EXPIRED, 만료",
            "REFUNDED, 환불"
    })
    @DisplayName("예매 상태의 설명이 올바르게 반환되는지 검증")
    void getDescription(ReservationStatus status, String expected) {
        // when
        String actual = status.getDescription();

        // then
        assertThat(actual).isEqualTo(expected);
    }
}
