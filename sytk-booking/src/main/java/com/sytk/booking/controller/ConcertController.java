package com.sytk.booking.controller;

import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ConcertCreateResponse> post(@RequestBody @Valid ConcertCreateRequest request) {
        ConcertCreateResponse response = concertService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
