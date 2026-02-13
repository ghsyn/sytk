package com.sytk.booking.service;

import com.sytk.booking.domain.Concert;
import com.sytk.booking.exception.ConcertNotFoundException;
import com.sytk.booking.repository.ConcertRepository;
import com.sytk.booking.response.ConcertDetailsResponse;
import com.sytk.booking.response.ConcertListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    public ConcertDetailsResponse getDetails(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);

        return ConcertDetailsResponse.from(concert);
    }

    public List<ConcertListResponse> getList() {
        return concertRepository.findAll().stream()
                .map(ConcertListResponse::from)
                .collect(Collectors.toList());
    }
}
