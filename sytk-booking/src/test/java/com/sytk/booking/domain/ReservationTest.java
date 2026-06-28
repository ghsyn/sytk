package com.sytk.booking.domain;

import com.sytk.booking.exception.InvalidReservationStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ReservationTest {

    @Mock
    private Seat seat;

    @Nested
    @DisplayName("예매 생성")
    class Create {

        @Test
        @DisplayName("[성공케이스] 유효한 유저 ID와 좌석으로 예매 생성 시 RESERVING 상태로 초기화")
        void create_success() {
            // when
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));

            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVING);
        }

        @Test
        @DisplayName("[실패케이스] userId가 null이면 IllegalArgumentException 예외 발생")
        void create_fail_nullUserId() {
            // when & then
            assertThatThrownBy(() -> Reservation.builder()
                    .userId(null)
                    .seat(seat)
                    .expiredAt(OffsetDateTime.now().plusMinutes(10))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유저 ID는 필수입니다.");
        }

        @Test
        @DisplayName("[실패케이스] seat이 null이면 IllegalArgumentException 예외 발생")
        void create_fail_nullSeat() {
            // when & then
            assertThatThrownBy(() -> Reservation.builder()
                    .userId(1L)
                    .seat(null)
                    .expiredAt(OffsetDateTime.now().plusMinutes(10))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("좌석은 필수입니다.");
        }

        @Test
        @DisplayName("[실패케이스] expiredAt이 null이면 IllegalArgumentException 예외 발생")
        void create_fail_nullExpiredAt() {
            // when & then
            assertThatThrownBy(() -> Reservation.builder()
                    .userId(1L)
                    .seat(seat)
                    .expiredAt(null)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("만료 시간은 필수입니다.");
        }
    }

    @Nested
    @DisplayName("상태 전이 성공케이스")
    class SuccessCases {

        @Test
        @DisplayName("[성공케이스] 대기 상태에서 결제 완료 시 확정 상태로 변경")
        void confirm_success() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));

            // when
            reservation.confirm();

            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("[성공케이스] 대기 상태에서 예매 취소 시 취소 상태로 변경")
        void cancel_success() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));

            // when
            reservation.cancel();

            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELED);
        }

        @Test
        @DisplayName("[성공케이스] 대기 상태에서 점유 만료 시 만료 상태로 변경")
        void expire_success() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));

            // when
            reservation.expire();

            // then
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("상태 전이 실패케이스")
    class FailureCases {

        @Test
        @DisplayName("[실패케이스] 이미 확정된 예매를 재확정하려 하면 InvalidReservationStatusTransitionException 예외 발생")
        void confirm_fail_alreadyConfirmed() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));
            reservation.confirm();

            // when & then
            assertThatThrownBy(reservation::confirm)
                    .isInstanceOf(InvalidReservationStatusTransitionException.class);
        }

        @Test
        @DisplayName("[실패케이스] 취소된 예매를 취소하려 하면 InvalidReservationStatusTransitionException 예외 발생")
        void cancel_fail_alreadyCanceled() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));
            reservation.cancel();

            // when & then
            assertThatThrownBy(reservation::cancel)
                    .isInstanceOf(InvalidReservationStatusTransitionException.class);
        }

        @Test
        @DisplayName("[실패케이스] 만료된 예매를 확정하려 하면 InvalidReservationStatusTransitionException 예외 발생")
        void confirm_fail_alreadyExpired() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));
            reservation.expire();

            // when & then
            assertThatThrownBy(reservation::confirm)
                    .isInstanceOf(InvalidReservationStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("만료 여부 확인")
    class IsExpired {

        @Test
        @DisplayName("[성공케이스] 만료 시간이 현재 시간보다 과거이면 만료된 예매로 판단")
        void isExpired_true() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().minusSeconds(1));

            // when & then
            assertThat(reservation.isExpiredAt(OffsetDateTime.now())).isTrue();
        }

        @Test
        @DisplayName("[성공케이스] 만료 시간이 현재 시간보다 미래이면 유효한 예매로 판단")
        void isExpired_false() {
            // given
            Reservation reservation = createReservation(OffsetDateTime.now().plusMinutes(10));

            // when & then
            assertThat(reservation.isExpiredAt(OffsetDateTime.now())).isFalse();
        }
    }

    /**
     * Helper Method
     */
    private Reservation createReservation(OffsetDateTime expiredAt) {
        return Reservation.builder()
                .userId(1L)
                .seat(seat)
                .expiredAt(expiredAt)
                .build();
    }
}
