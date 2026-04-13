package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.domain.ConcertEditor;
import com.sytk.booking.exception.ConcertNotFoundException;
import com.sytk.booking.exception.ConcertPolicyException;
import com.sytk.booking.exception.DuplicateConcertException;
import com.sytk.booking.exception.NotChangedException;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.repository.ReservationRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.request.ConcertEditRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.response.ConcertEditResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    private final ReservationRepository reservationRepository;

    /**
     * 공연 등록
     */
    public ConcertCreateResponse create(ConcertCreateRequest request) {
        if (concertRepository.existsByTitle(request.title())) {
            throw new DuplicateConcertException();
        }

        return ConcertCreateResponse.from(concertRepository.save(request.toEntity()));
    }

    /**
     * 공연 수정
     */
    public ConcertEditResponse edit(Long id, ConcertEditRequest request) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);

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

    public void delete(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);

        if (reservationRepository.existsByConcertId(id)) {
            throw new ConcertPolicyException();
        }

        concertRepository.delete(concert);
    }
}
