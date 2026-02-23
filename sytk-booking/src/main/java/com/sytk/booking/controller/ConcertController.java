package com.sytk.booking.controller;

import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.response.ConcertDetailsResponse;
import com.sytk.booking.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Concert 도메인 API 제공
 */
@RestController
@RequiredArgsConstructor
public class ConcertController {

    private final ConcertService concertService;

    /**
     * 공연 등록
     */
    @PostMapping("/api/v1/concert")
    public ConcertDetailsResponse post(@RequestBody @Valid ConcertCreateRequest request) {
        return concertService.create(request);
    }
}
