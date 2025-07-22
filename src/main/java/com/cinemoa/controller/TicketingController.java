package com.cinemoa.controller;

import com.cinemoa.dto.MovieDto;
import com.cinemoa.dto.ShowtimeDto;
import com.cinemoa.entity.Cinema;
import com.cinemoa.entity.Movie;
import com.cinemoa.entity.Showtime;
import com.cinemoa.repository.ReservationSeatRepository;
import com.cinemoa.repository.SeatRepository;
import com.cinemoa.service.CinemaService;
import com.cinemoa.service.MovieService;
import com.cinemoa.service.ShowtimeService;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

@Controller
@RequestMapping("/ticketing")
public class TicketingController {

    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final ShowtimeService showtimeService;

    // ✅추가
    private final SeatRepository seatRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    @Autowired
    public TicketingController(MovieService movieService,
                               CinemaService cinemaService,
                               ShowtimeService showtimeService,
                               SeatRepository seatRepository,//✅추가
                               ReservationSeatRepository reservationSeatRepository) {
        this.movieService = movieService;
        this.cinemaService = cinemaService;
        this.showtimeService = showtimeService;
        //✅추가
        this.seatRepository = seatRepository;
        this.reservationSeatRepository = reservationSeatRepository;
    }

    @GetMapping("")
    public String ticketingPage(Model model) {
        // 모든 영화 가져오기
        List<MovieDto> allMovies = movieService.getAllMovies();

        Long firstMovieId = null;
        if (!allMovies.isEmpty()) {
            firstMovieId = allMovies.get(0).getMovieId();
        }

        return commonTicketingLogic(firstMovieId, model);
    }

    @GetMapping("/{movieId}")
    public String ticketingByMovie(@PathVariable("movieId") Long movieId, Model model) {
        return commonTicketingLogic(movieId, model);
    }

    // 공통 로직을 처리하는 private 메서드
    private String commonTicketingLogic(Long selectedMovieId, Model model) {
        // 상영 중인 모든 영화 목록 가져오기 (페이징 없이 모든 데이터)
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("title").ascending());
        List<MovieDto> nowShowingMovies = movieService.getMoviesByScreeningStatus(
                        Movie.ScreeningStatus.NOW_SHOWING, pageable, null)
                .getContent();

        // 모든 영화 목록
        List<MovieDto> allMovies = movieService.getAllMovies();

        model.addAttribute("movies", allMovies);
        model.addAttribute("nowShowingMovies", nowShowingMovies);
        model.addAttribute("selectedMovieId", selectedMovieId);

        // 모든 영화관 정보 가져오기
        List<Cinema> cinemas = cinemaService.getAllCinemas();
        model.addAttribute("cinemas", cinemas);

        // 지역별 영화관 목록 가져오기
        List<String> regions = cinemas.stream()
                .sorted((c1, c2) -> Long.compare(c1.getCinemaId(), c2.getCinemaId())) // Cinema ID 오름차순 정렬
                .map(Cinema::getRegion)
                .distinct()
                .toList();
        model.addAttribute("regions", regions);

        model.addAttribute("title", "영화 예매");
        model.addAttribute("timestamp", System.currentTimeMillis());

        return "ticketing/ticketing"; // ticketing.mustache 템플릿 렌더링
    }

    // AJAX API: 특정 영화, 영화관에 대한 상영 가능한 날짜 목록 조회
    @GetMapping("/api/dates")
    @ResponseBody
    public List<String> getAvailableDates(
            @RequestParam Long movieId,
            @RequestParam Long cinemaId) {
        return showtimeService.getAvailableDatesByMovieAndCinema(movieId, cinemaId);
    }

    // AJAX API: 특정 영화, 영화관, 날짜에 대한 상영 시간 목록 조회
    @GetMapping("/api/showtimes")
    @ResponseBody
    public List<Map<String, Object>> getShowtimes(
            @RequestParam Long movieId,
            @RequestParam Long cinemaId,
            @RequestParam String date) {

        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        List<ShowtimeDto> showtimes = showtimeService.getShowtimesByMovieCinemaAndDate(movieId, cinemaId, localDate);


        return showtimes.stream().map(showtime -> {
            Map<String, Object> showtimeMap = new HashMap<>();
            showtimeMap.put("showtimeId", showtime.getShowtimeId());
            showtimeMap.put("startTime", showtime.getStartTime());
            showtimeMap.put("endTime", showtime.getEndTime());
            showtimeMap.put("screenName", showtime.getScreenName());

            // ✅ 남은 좌석 수 계산 추가
            int totalSeats = seatRepository.countByScreen_ScreenId(showtime.getScreenId());
            int reservedSeats = reservationSeatRepository.countByShowtime_ShowtimeId(showtime.getShowtimeId());
            int availableSeats = totalSeats - reservedSeats;
            showtimeMap.put("availableSeats", availableSeats); // ← 프론트로 전달

            return showtimeMap;
        }).collect(Collectors.toList());
    }

    // 지역별 영화관 목록 조회
    @GetMapping("/api/cinemas")
    @ResponseBody
    public List<Cinema> getCinemasByRegion(@RequestParam String region) {
        return cinemaService.getCinemasByRegion(region);
    }

}