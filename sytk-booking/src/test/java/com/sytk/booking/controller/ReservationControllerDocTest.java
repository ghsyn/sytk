package com.sytk.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sytk.booking.domain.ReservationStatus;
import com.sytk.booking.request.ReservationCreateRequest;
import com.sytk.booking.response.ReservationCreateResponse;
import com.sytk.booking.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureRestDocs(uriScheme = "http", uriHost = "localhost", uriPort = 8080)
@ExtendWith(RestDocumentationExtension.class)
class ReservationControllerDocTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void reservation_create() throws Exception {
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("reservation-create",
                        requestFields(
                                fieldWithPath("userId").description("예매하는 유저 ID"),
                                fieldWithPath("seatId").description("예매할 좌석 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("생성된 예매 ID"),
                                fieldWithPath("status").description("예매 상태 (RESERVING)")
                        )
                ));
    }

    @Test
    void reservation_cancel() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/cancel", 1L))
                .andExpect(status().isNoContent())
                .andDo(document("reservation-cancel",
                        pathParameters(
                                parameterWithName("id").description("취소할 예매 ID")
                        )
                ));
    }

    @Test
    void reservation_confirm() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/confirm", 1L))
                .andExpect(status().isNoContent())
                .andDo(document("reservation-confirm",
                        pathParameters(
                                parameterWithName("id").description("확정할 예매 ID")
                        )
                ));
    }

    @Test
    void reservation_expire() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/expire", 1L))
                .andExpect(status().isNoContent())
                .andDo(document("reservation-expire",
                        pathParameters(
                                parameterWithName("id").description("만료 처리할 예매 ID")
                        )
                ));
    }

    @Test
    void reservation_refund() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/v1/reservations/{id}/refund", 1L))
                .andExpect(status().isNoContent())
                .andDo(document("reservation-refund",
                        pathParameters(
                                parameterWithName("id").description("환불 처리할 예매 ID")
                        )
                ));
    }
}
