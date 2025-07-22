// src/main/java/com/cinemoa/service/impl/ShowtimeServiceImpl.java
package com.cinemoa.service;

import com.cinemoa.dto.ShowtimeDto;
import com.cinemoa.entity.Showtime;
import com.cinemoa.repository.ShowtimeRepository;
import com.cinemoa.service.ShowtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // 날짜 포맷을 위해 임포트
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;

    @Autowired
    public ShowtimeServiceImpl(ShowtimeRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public List<String> getAvailableDatesByMovieAndCinema(Long movieId, Long cinemaId) {
        List<LocalDate> dates = showtimeRepository.findDistinctDatesByMovieAndCinema(movieId, cinemaId);
        // "yyyy-MM-dd" 형식의 문자열로 변환하여 반환
        return dates.stream()
                .map(date -> date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowtimeDto> getShowtimesByMovieCinemaAndDate(Long movieId, Long cinemaId, LocalDate date) {
        // LocalDate를 LocalDateTime 범위로 변환
        LocalDateTime startOfDay = date.atStartOfDay(); // 해당 날짜의 00:00:00
        LocalDateTime endOfDay = date.atTime(23, 59, 59); // 해당 날짜의 23:59:59

        // 파생 쿼리 메서드 호출
        List<Showtime> showtimes = showtimeRepository.findByMovie_MovieIdAndScreen_Cinema_CinemaIdAndStartTimeBetweenOrderByStartTimeAsc(
                movieId, cinemaId, startOfDay, endOfDay
        );

        return showtimes.stream().map(showtime -> {
            LocalDateTime calculatedEndTime = null;
            if (showtime.getMovie() != null && showtime.getMovie().getRunningTime() != null) {
                calculatedEndTime = showtime.getStartTime().plusMinutes(showtime.getMovie().getRunningTime());
            } else {
                calculatedEndTime = showtime.getEndTime();
            }

            return ShowtimeDto.builder()
                    .showtimeId(showtime.getShowtimeId())
                    .startTime(showtime.getStartTime())
                    .endTime(calculatedEndTime)
                    .screenName(showtime.getScreen().getScreenName())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Showtime> getShowtimeById(Long showtimeId) {
        // 파생 쿼리 메서드 findByShowtimeId 호출
        return showtimeRepository.findByShowtimeId(showtimeId);
    }
}