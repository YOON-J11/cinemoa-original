package com.cinemoa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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


    private Long reservationId; // 예매 고유번호
    private String memberId; // 회원 ID
    private Long movieId; // 영화 ID
    private Long cinemaId; // 영화관 ID
    private Long screenId; // 상영관 ID
    private String seatInfo; // 좌석 정보 (예: A5,A6)
    private LocalDateTime reservationTime; // 예매 시간
    private String paymentMethod; // 결제 수단
    private String status; // 예매 상태
    private String movieTitle; // 영화 이름 (조인)
    private String cinemaName; // 영화관 이름 (조인)
    private String mainImageUrl; // 영화 대표 이미지 파일명
    private String formattedPaymentDate; // 결제일
    private LocalDateTime showtimeStart; // 상영 시작시간
    private String formattedShowtimeStart; // 상영 종료 시간
    private String seatTypeSummary; // 좌석타입 예: STANDARD 2
    private String formattedReservationDate; // yyyy-MM-dd 형식만 표시



    // 포맷된 예매일 반환용 getter
    public String getFormattedReservationTime() {
        if (reservationTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm");
        return reservationTime.format(formatter);
    }


    public String getStatusClass() {
        if (status == null) return "unknown";
        return switch (status) {
            case "예약완료" -> "confirmed";
            case "취소됨" -> "cancelled";
            default -> "unknown";
        };
    }



}
