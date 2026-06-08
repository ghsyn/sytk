package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.domain.ConcertEditor;
import com.sytk.booking.domain.Seat;
import com.sytk.booking.domain.SeatGrade;
import com.sytk.booking.exception.*;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.repository.ReservationQueryRepository;
import com.sytk.booking.repository.SeatGradeRepository;
import com.sytk.booking.repository.SeatRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.request.ConcertEditRequest;
import com.sytk.booking.request.SeatGradeCreateRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.response.ConcertEditResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    private final SeatGradeRepository seatGradeRepository;

    private final SeatRepository seatRepository;

    private final ReservationQueryRepository reservationQueryRepository;

    /**
     * 공연 등록
     */
    @Transactional
    public ConcertCreateResponse create(ConcertCreateRequest request) {
        if (concertRepository.existsByTitle(request.title())) {
            throw new DuplicateConcertException();
        }

        if (request.hasSeatGradeList()) {
            long distinctCount = request.seatGradeList().stream()
                    .map(SeatGradeCreateRequest::name)
                    .distinct()
                    .count();

            if (distinctCount != request.seatGradeList().size()) {
                throw new DuplicateSeatGradeException();
            }
        }

        try {
            Concert concert = concertRepository.save(request.toEntity());

            if (request.hasSeatGradeList()) {
                List<SeatGrade> seatGradeList = request.seatGradeList().stream()
                        .map(seatGradeDto -> seatGradeDto.toEntity(concert))
                        .toList();

                seatGradeRepository.saveAll(seatGradeList);

                List<Seat> seatList = seatGradeList.stream()
                        .flatMap(seatGrade -> seatGrade.createSeats().stream())
                        .toList();

                if (!seatList.isEmpty()) {
                    seatRepository.saveAll(seatList);   // 좌석 수 많은 경우 성능 테스트 필요 -> 성능 저하 시 하나의 쿼리로 묶어 던지도록 리팩토링 고려
                }
            }

            return ConcertCreateResponse.from(concert);

        } catch (DataIntegrityViolationException e) {   // 동시 동일한 공연, 좌석 등급 등록 상황에서 DB 유니크 제약 조건 위반 시 발생
            if (e.getMessage().contains("seat_grade") || e.getMessage().contains("UK_SEAT_GRADE")) {
                throw new DuplicateSeatGradeException();
            }
            throw new DuplicateConcertException();
        }
    }

    /**
     * 공연 수정
     */
    @Transactional
    public ConcertEditResponse edit(Long id, ConcertEditRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);

        if (request.title() != null && !concert.getTitle().equals(request.title())
                && concertRepository.existsByTitle(request.title())) {
                throw new DuplicateConcertException();
            }

        var editorBuilder = concert.toEditor();

        if (request.title() != null) editorBuilder.title(request.title());
        if (request.startAt() != null) editorBuilder.startAt(request.startAt());
        if (request.venue() != null) editorBuilder.venue(request.venue());
        if (request.runningTime() != null) editorBuilder.runningTime(request.runningTime());
        if (request.ticketOpenAt() != null) editorBuilder.ticketOpenAt(request.ticketOpenAt());
        if (request.ticketCloseAt() != null) editorBuilder.ticketCloseAt(request.ticketCloseAt());

        ConcertEditor newEditor = editorBuilder.build();

        if (isNotChanged(concert, newEditor)) {
            throw new NotChangedException();
        }

        concert.edit(newEditor);

        return ConcertEditResponse.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .build();
    }

    private boolean isNotChanged(Concert concert, ConcertEditor editor) {
        return Objects.equals(concert.getTitle(), editor.title()) &&
                Objects.equals(concert.getStartAt(), editor.startAt()) &&
                Objects.equals(concert.getVenue(), editor.venue()) &&
                Objects.equals(concert.getRunningTime(), editor.runningTime()) &&
                Objects.equals(concert.getTicketOpenAt(), editor.ticketOpenAt()) &&
                Objects.equals(concert.getTicketCloseAt(), editor.ticketCloseAt());
    }

    /**
     * 공연 삭제
     */
    @Transactional
    public void delete(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);

        if (reservationQueryRepository.existsByConcertId(id)) {
            throw new ConcertPolicyException();
        }

        try {
            concertRepository.delete(concert);
            concertRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConcertPolicyException();  // 삭제와 예매가 동시에 이루어지는 상황에서 삭제 조건 위반 시 발생
        }
    }
}
