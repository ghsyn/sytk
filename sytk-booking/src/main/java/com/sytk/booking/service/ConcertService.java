package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.domain.ConcertEditor;
import com.sytk.booking.exception.ConcertNotFoundException;
import com.sytk.booking.exception.ConcertPolicyException;
import com.sytk.booking.exception.DuplicateConcertException;
import com.sytk.booking.exception.NotChangedException;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.repository.ReservationQueryRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.request.ConcertEditRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.response.ConcertEditResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    private final ReservationQueryRepository reservationQueryRepository;

    /**
     * 공연 등록
     */
    public ConcertCreateResponse create(ConcertCreateRequest request) {
        if (concertRepository.existsByTitle(request.title())) {
            throw new DuplicateConcertException();
        }

        try {
            Concert concert = concertRepository.save(request.toEntity());
            return ConcertCreateResponse.from(concert);
        } catch (DataIntegrityViolationException e) {   // 동시 동일 제목 공연 등록 상황에서 DB 유니크 제약 조건 위반 시 발생
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

        if (concertRepository.existsByTitle(request.title())) {
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
