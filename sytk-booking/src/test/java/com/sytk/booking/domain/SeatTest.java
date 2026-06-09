package com.sytk.booking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatTest {

    @Mock
    private SeatGrade seatGrade;

    @Nested
    @DisplayName("상태 전이 성공케이스")
    class successCases {

        @Test
        @DisplayName("[성공케이스] 미판매 좌석을 개시하여 예약 가능 상태로 변경")
        void open_success() {
            // given
            Seat seat = createSeat(SeatStatus.CLOSED);

            // when
            seat.open();

            // then
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("[성공케이스] 예약 가능 좌석을 선점하여 예약 중 상태로 변경")
        void hold_success() {
            // given
            Seat seat = createSeat(SeatStatus.AVAILABLE);

            // when
            seat.hold();

            // then
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.OCCUPIED);
        }

        @Test
        @DisplayName("[성공케이스] 점유 중 좌석 결제 취소 혹은 타임 아웃 시 예약 가능 상태로 변경")
        void release_success() {
            // given
            Seat seat = createSeat(SeatStatus.OCCUPIED);

            // when
            seat.release();

            // then
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("[성공케이스] 점유 중 좌석 결제 완료 시 판매 완료 상태로 변경")
        void sell_success() {
            // given
            Seat seat = createSeat(SeatStatus.OCCUPIED);

            // when
            seat.sell();

            // then
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD);
        }

        @Test
        @DisplayName("[성공케이스] 열린 좌석 이용 불가 시 미판매 상태로 변경")
        void close_success() {
            // given
            Seat seat = createSeat(SeatStatus.AVAILABLE);

            // when
            seat.close();

            // then
            assertThat(seat.getStatus()).isEqualTo(SeatStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("상태 전이 실패케이스")
    class failureCases {

        @ParameterizedTest(name = "{0} 상태에서 {1} 시도할 경우 IllegalStateException 발생")
        @CsvSource({
                "OCCUPIED, open, 선점, 예약 가능",
                "CLOSED, hold, 미판매, 선점",
                "SOLD, release, 판매 완료, 예약 가능",
                "AVAILABLE, sell, 예약 가능, 판매 완료",
                "SOLD, close, 판매 완료, 미판매"
        })
        @DisplayName("[실패케이스] 상태 전이 비즈니스 규칙 위반 시 올바른 예외 및 메시지 반환")
        void changeStatus_fail(SeatStatus current, String action, String fromDesc, String toDesc) {
            // given
            Seat seat = createSeat(current);

            // when & then
            assertThatThrownBy(() -> {
                switch (action) {
                    case "open" -> seat.open();
                    case "hold" -> seat.hold();
                    case "release" -> seat.release();
                    case "sell" -> seat.sell();
                    case "close" -> seat.close();
                    default -> throw new IllegalArgumentException("정의되지 않는 메서드: " + action);
                }
            })
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(String.format("좌석 상태를 %s에서 %s(으)로 변경할 수 없습니다.", fromDesc, toDesc));
        }
    }

    /**
     * Helper Method
     */
    private Seat createSeat(SeatStatus status) {
        return Seat.builder()
                .number(1)
                .status(status)
                .seatGrade(seatGrade)
                .build();
    }
}