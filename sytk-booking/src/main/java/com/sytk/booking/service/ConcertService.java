package com.sytk.booking.service;

import com.sytk.booking.exception.DuplicateConcertException;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    /**
     * 공연 등록
     */
    public ConcertCreateResponse create(ConcertCreateRequest request) {
        if (concertRepository.existsByTitle(request.title())) {
            throw new DuplicateConcertException();
        }

        return ConcertCreateResponse.from(concertRepository.save(request.toEntity()));
    }
}
