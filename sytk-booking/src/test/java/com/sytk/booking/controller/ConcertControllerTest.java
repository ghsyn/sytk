package com.sytk.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sytk.booking.exception.ErrorCode;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.service.ConcertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConcertController.class)
class ConcertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConcertService concertService;

    @Test
    @DisplayName("공연 등록 시 제목이 없으면 INVALID_REQUEST 에러가 발생한다.")
    void create_fail_titleEmpty() throws Exception {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .startAt(OffsetDateTime.now())
                .venue("foo")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/concert")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.validation.title").value("제목을 입력하세요."))
                .andDo(print());

        verify(concertService, never()).create(any());
    }

    @Test
    @DisplayName("공연 등록 시 장소가 10 ~ 255자가 아니면 INVALID_REQUEST 에러가 발생한다.")
    void create_fail_venueLength() throws Exception {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("foo")
                .startAt(OffsetDateTime.now())
                .venue("공연 장소입니다.")     // 9글자
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/concert")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.validation.venue").value("장소는 10 ~ 255 글자로 입력해주세요."))
                .andDo(print());

        verify(concertService, never()).create(any());
    }

}