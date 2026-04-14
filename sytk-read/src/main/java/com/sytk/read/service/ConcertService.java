package com.sytk.read.service;

import com.sytk.read.domain.Concert;
import com.sytk.read.exception.ConcertNotFoundException;
import com.sytk.read.repository.ConcertRepository;
import com.sytk.read.response.ConcertDetailsResponse;
import com.sytk.read.response.ConcertListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    /**
     * 공연 단건 조회
     * 시스템용, 실제 사용자 접근 불가
     */
    public ConcertDetailsResponse getDetails(Long id) {
        Concert concert = concertRepository.findById(id)
                .orElseThrow(ConcertNotFoundException::new);

        return ConcertDetailsResponse.from(concert);
    }

    /**
     * 공연 목록 조회
     * 시스템용, 실제 사용자 접근 불가
     */
    public List<ConcertListResponse> getList() {
        return concertRepository.findAll().stream()
                .map(ConcertListResponse::from)
                .toList();
    }
}
