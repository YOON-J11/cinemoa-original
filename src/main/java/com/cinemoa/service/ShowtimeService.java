// src/main/java/com/cinemoa/service/ShowtimeService.java
package com.cinemoa.service;

import com.cinemoa.dto.ShowtimeDto;
import com.cinemoa.entity.Showtime;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShowtimeService {
    // 특정 영화와 영화관에 대해 상영 가능한 날짜 목록 조회 (새로운 메서드)
    List<String> getAvailableDatesByMovieAndCinema(Long movieId, Long cinemaId);

    // API 컨트롤러에서 호출할 메서드 (DTO 반환)
    List<ShowtimeDto> getShowtimesByMovieCinemaAndDate(Long movieId, Long cinemaId, LocalDate date);

    // 좌석 선택 페이지 등에서 사용할 메서드 (엔티티 반환)
    Optional<Showtime> getShowtimeById(Long showtimeId);
}