package com.sytk.booking.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.sytk.booking.domain.QReservation.reservation;
import static com.sytk.booking.domain.QSeat.seat;
import static com.sytk.booking.domain.QSeatGrade.seatGrade;

@Repository
@RequiredArgsConstructor
public class ReservationQueryRepository {

    private final JPAQueryFactory queryFactory;

    public boolean existsByConcertId(Long concertId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(reservation)
                .join(seat).on(reservation.seatId.eq(seat.id))
                .join(seat.seatGrade, seatGrade)
                .where(seatGrade.concert.id.eq(concertId))
                .fetchFirst();

        return fetchOne != null;
    }
}
