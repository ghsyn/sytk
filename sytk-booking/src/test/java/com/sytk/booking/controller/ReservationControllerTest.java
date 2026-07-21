package com.sytk.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sytk.booking.domain.ReservationStatus;
import com.sytk.booking.exception.ReservationNotFoundException;
import com.sytk.booking.request.ReservationCancelRequest;
import com.sytk.booking.request.ReservationConfirmRequest;
import com.sytk.booking.request.ReservationCreateRequest;
import com.sytk.booking.request.ReservationExpireRequest;
import com.sytk.booking.request.ReservationRefundRequest;
import com.sytk.booking.response.ReservationCreateResponse;
import com.sytk.booking.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.sytk.booking.exception.ErrorCode.INVALID_REQUEST;
import static com.sytk.booking.exception.ErrorCode.RESERVATION_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    /**
     * 예매 생성 테스트
     */
    @Test
    @DisplayName("[성공케이스] 예매 생성 시 201 상태코드 및 ID, 상태(RESERVING) 반환")
    void create_success() throws Exception {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .userId(1L)
                .seatId(1L)
                .build();

        ReservationCreateResponse response = ReservationCreateResponse.builder()
                .id(1L)
                .status(ReservationStatus.RESERVING)
                .build();
        given(reservationService.reserve(any(ReservationCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value(ReservationStatus.RESERVING.name()))
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).reserve(any(ReservationCreateRequest.class));
    }

    @Test
    @DisplayName("[실패케이스 - 필드 유효성 검증] 예매 생성 시 유저 ID가 없으면 INVALID_REQUEST 에러 발생")
    void create_fail_userIdNull() throws Exception {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .seatId(1L)
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value(INVALID_REQUEST.getMessage()))
                .andExpect(jsonPath("$.validation.userId").value("유저 ID를 입력하세요."))
                .andDo(print());

        // verify
        then(reservationService).should(never()).reserve(any());
    }

    @Test
    @DisplayName("[실패케이스 - 필드 유효성 검증] 예매 생성 시 좌석 ID가 없으면 INVALID_REQUEST 에러 발생")
    void create_fail_seatIdNull() throws Exception {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .userId(1L)
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value(INVALID_REQUEST.getMessage()))
                .andExpect(jsonPath("$.validation.seatId").value("좌석 ID를 입력하세요."))
                .andDo(print());

        // verify
        then(reservationService).should(never()).reserve(any());
    }

    /**
     * 예매 취소 테스트
     */
    @Test
    @DisplayName("[성공케이스] 예매 취소 시 204 상태코드 반환")
    void cancel_success() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationCancelRequest request = ReservationCancelRequest.builder().id(reservationId).build();

        willDoNothing().given(reservationService).cancel(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/cancel", reservationId))
                .andExpect(status().isNoContent())
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).cancel(eq(request));
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 취소 시 RESERVATION_NOT_FOUND 에러 발생")
    void cancel_fail_notFound() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationCancelRequest request = ReservationCancelRequest.builder().id(reservationId).build();

        willThrow(new ReservationNotFoundException()).given(reservationService).cancel(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/cancel", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(RESERVATION_NOT_FOUND.getStatus().value()))
                .andExpect(jsonPath("$.message").value(RESERVATION_NOT_FOUND.getMessage()))
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).cancel(eq(request));
    }

    @Test
    @DisplayName("[실패케이스 - 형식 오류] 요청 URI의 예매 ID 값에 숫자가 아닌 값 입력 시 INVALID_REQUEST 에러 발생")
    void cancel_fail_invalidId() throws Exception {
        // given
        String invalidId = "not-a-number";

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/cancel", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(INVALID_REQUEST.getStatus().value()))
                .andExpect(jsonPath("$.message").value(INVALID_REQUEST.getMessage()))
                .andDo(print());

        // verify
        then(reservationService).should(never()).cancel(any());
    }

    /**
     * 예매 확정 테스트
     */
    @Test
    @DisplayName("[성공케이스] 예매 확정 시 204 상태코드 반환")
    void confirm_success() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationConfirmRequest request = ReservationConfirmRequest.builder().id(reservationId).build();

        willDoNothing().given(reservationService).confirm(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/confirm", reservationId))
                .andExpect(status().isNoContent())
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).confirm(eq(request));
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 확정 시 RESERVATION_NOT_FOUND 에러 발생")
    void confirm_fail_notFound() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationConfirmRequest request = ReservationConfirmRequest.builder().id(reservationId).build();

        willThrow(new ReservationNotFoundException()).given(reservationService).confirm(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/confirm", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(RESERVATION_NOT_FOUND.getStatus().value()))
                .andExpect(jsonPath("$.message").value(RESERVATION_NOT_FOUND.getMessage()))
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).confirm(eq(request));
    }

    /**
     * 예매 만료 테스트
     */
    @Test
    @DisplayName("[성공케이스] 예매 만료 처리 시 204 상태코드 반환")
    void expire_success() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationExpireRequest request = ReservationExpireRequest.builder().id(reservationId).build();

        willDoNothing().given(reservationService).expire(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/expire", reservationId))
                .andExpect(status().isNoContent())
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).expire(eq(request));
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 만료 처리 시 RESERVATION_NOT_FOUND 에러 발생")
    void expire_fail_notFound() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationExpireRequest request = ReservationExpireRequest.builder().id(reservationId).build();

        willThrow(new ReservationNotFoundException()).given(reservationService).expire(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/expire", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(RESERVATION_NOT_FOUND.getStatus().value()))
                .andExpect(jsonPath("$.message").value(RESERVATION_NOT_FOUND.getMessage()))
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).expire(eq(request));
    }

    /**
     * 예매 환불 테스트
     */
    @Test
    @DisplayName("[성공케이스] 예매 환불 시 204 상태코드 반환")
    void refund_success() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationRefundRequest request = ReservationRefundRequest.builder().id(reservationId).build();

        willDoNothing().given(reservationService).refund(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/refund", reservationId))
                .andExpect(status().isNoContent())
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).refund(eq(request));
    }

    @Test
    @DisplayName("[실패케이스 - 접근 검증] 존재하지 않는 예매 환불 시 RESERVATION_NOT_FOUND 에러 발생")
    void refund_fail_notFound() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationRefundRequest request = ReservationRefundRequest.builder().id(reservationId).build();

        willThrow(new ReservationNotFoundException()).given(reservationService).refund(request);

        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/refund", reservationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(RESERVATION_NOT_FOUND.getStatus().value()))
                .andExpect(jsonPath("$.message").value(RESERVATION_NOT_FOUND.getMessage()))
                .andDo(print());

        // verify
        then(reservationService).should(times(1)).refund(eq(request));
    }
}
