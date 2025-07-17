package com.cinemoa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {

    public ReservationDto(String movieTitle, String cinemaName, String reservationTime) {
        this.movieTitle = movieTitle;
        this.cinemaName = cinemaName;
        this.reservationTime = LocalDateTime.parse(reservationTime);
    }


    private Long reservationId;           // 예매 고유번호
    private String memberId;             // 회원 ID
    private Long movieId;                // 영화 ID
    private Long cinemaId;         // 영화관 ID
    private Integer screenId;            // 상영관 ID
    private String seatInfo;             // 좌석 정보 (예: A5,A6)
    private LocalDateTime reservationTime; // 예매 시간
    private String paymentMethod;        // 결제 수단
    private String status;               // 예매 상태
    private String movieTitle;  // 영화 이름 (조인)
    private String cinemaName;  // 영화관 이름 (조인)


}
