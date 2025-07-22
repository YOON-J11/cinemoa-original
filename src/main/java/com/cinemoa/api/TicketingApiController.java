package com.cinemoa.api;

import com.cinemoa.dto.ShowtimeDto; // DTO 임포트
import com.cinemoa.entity.Cinema; // Cinema 엔티티 임포트
import com.cinemoa.service.CinemaService;
import com.cinemoa.service.ShowtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/ticketing") // 예매 관련 API 엔드포인트들을 묶습니다.
public class TicketingApiController {

    private final CinemaService cinemaService;
    private final ShowtimeService showtimeService;

    @Autowired
    public TicketingApiController(CinemaService cinemaService, ShowtimeService showtimeService) {
        this.cinemaService = cinemaService;
        this.showtimeService = showtimeService;
    }

    // AJAX API: 특정 영화, 영화관에 대한 상영 가능한 날짜 목록 조회
    @GetMapping("/dates")
    public List<String> getAvailableDates(@RequestParam Long movieId, @RequestParam Long cinemaId) {
        return showtimeService.getAvailableDatesByMovieAndCinema(movieId, cinemaId);
    }

    // AJAX API: 특정 영화, 영화관, 날짜에 대한 상영 시간 목록 조회
    @GetMapping("/showtimes")
    public List<ShowtimeDto> getShowtimes( // DTO를 반환하도록 수정
                                                   @RequestParam Long movieId,
                                                   @RequestParam Long cinemaId,
                                                   @RequestParam String date) {

        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // ShowtimeService에서 이미 DTO로 변환하여 반환하므로 바로 사용
        return showtimeService.getShowtimesByMovieCinemaAndDate(movieId, cinemaId, localDate);
    }

    // AJAX API: 지역별 영화관 목록 조회
    @GetMapping("/cinemas")
    public List<Cinema> getCinemasByRegion(@RequestParam String region) {
        return cinemaService.getCinemasByRegion(region);
    }
}