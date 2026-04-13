package com.sytk.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sytk.booking.request.ConcertCreateRequest;
import com.sytk.booking.request.ConcertEditRequest;
import com.sytk.booking.response.ConcertCreateResponse;
import com.sytk.booking.response.ConcertEditResponse;
import com.sytk.booking.service.ConcertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.sytk.booking.exception.ErrorCode.INVALID_REQUEST;
import static java.time.OffsetDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    /**
     * 공연 등록 테스트
     */
    @Test
    @DisplayName("[성공케이스] 공연 등록 시 201 상태코드 및 ID, 제목 반환")
    void create_success() throws Exception {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("foo")
                .startAt(now())
                .venue("barrrrrrrr")
                .build();

        ConcertCreateResponse response = new ConcertCreateResponse(1L, "foo");
        given(concertService.create(any(ConcertCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/concert")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("foo"))
                .andDo(print());

        // verify
        then(concertService).should(times(1)).create(any(ConcertCreateRequest.class));
    }

    @Test
    @DisplayName("[실패케이스 - 필드 유효성 검증1] 공연 등록 시 제목이 없으면 INVALID_REQUEST 에러 발생")
    void create_fail_titleEmpty() throws Exception {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .startAt(now())
                .venue("foo")
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/concert")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.validation.title").value("제목을 입력하세요."))
                .andDo(print());

        // verify
        then(concertService).should(never()).create(any());
    }

    @Test
    @DisplayName("[실패케이스 - 필드 유효성 검증2] 공연 등록 시 장소가 10 ~ 255자가 아니면 INVALID_REQUEST 에러 발생")
    void create_fail_venueLength() throws Exception {
        // given
        ConcertCreateRequest request = ConcertCreateRequest.builder()
                .title("foo")
                .startAt(now())
                .venue("공연 장소입니다.")     // 9글자
                .build();

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/v1/concert")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.validation.venue").value("장소는 10 ~ 255 글자로 입력해주세요."))
                .andDo(print());

        // verify
        then(concertService).should(never()).create(any());
    }

    /**
     * 공연 수정 테스트
     */
    @Test
    @DisplayName("[성공케이스] 공연 제목 수정 시 해당 공연의 ID 및 변경 후 제목 반환")
    void edit_success() throws Exception{
        // given
        Long concertId = 1L;
        ConcertEditRequest request = ConcertEditRequest.builder()
                .title("new title")
                .startAt(now())
                .venue("10글자 이상의 공연 장소")
                .build();

        ConcertEditResponse response = ConcertEditResponse.builder()
                        .id(concertId)
                        .title("new title")
                        .build();

        given(concertService.edit(eq(concertId), any(ConcertEditRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/concert/{id}", concertId)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(concertId))
                .andExpect(jsonPath("$.title").value("new title"))
                .andDo(print());

        // verify
        then(concertService).should(times(1)).edit(eq(concertId), any(ConcertEditRequest.class));
    }

    @Test
    @DisplayName("[실패케이스 - 형식 오류] 요청 URI의 공연 ID 값에 숫자가 아닌 값 입력 시 INVALID_REQUEST 에러 발생")
    void edit_fail_invalidId() throws Exception {
        // given
        String invalidId = "not-a-number";
        ConcertEditRequest request = ConcertEditRequest.builder()
                .title("new title")
                .startAt(now())
                .venue("10글자 이상의 공연 장소")
                .build();

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/concert/{id}", invalidId)
                    .contentType(APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("[실패케이스 - 필드 유효성 검증] 제목, 시작시간, 장소 중 하나라도 빈 값을 저장할 시 INVALID_REQUEST 에러 발생")
    void edit_fail_emptyField() throws Exception {
        // given
        Long concertId = 1L;
        ConcertEditRequest request = ConcertEditRequest.builder()
                .title("new title")
                .venue("10글자 이상의 공연 장소")
                .build();

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/concert/{id}", concertId)
                    .contentType(APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."))
                .andExpect(jsonPath("$.validation.startAt").value("시작시간을 입력하세요."))
                .andDo(print());

        // verify
        then(concertService).should(never()).edit(eq(concertId), any(ConcertEditRequest.class));
    }

}