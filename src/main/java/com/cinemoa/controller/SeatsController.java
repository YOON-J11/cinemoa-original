package com.cinemoa.controller;

import com.cinemoa.entity.Reservation;
import com.cinemoa.entity.Seat;
import com.cinemoa.repository.ReservationRepository;
import com.cinemoa.repository.SeatRepository;
import com.cinemoa.entity.Showtime;
import com.cinemoa.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class SeatsController {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping("/seats")
    public String showSeatPage(@RequestParam("showtimeId") Long showtimeId, Model model) {
        Optional<Showtime> showtimeOptional = showtimeRepository.findById(showtimeId);

        if (showtimeOptional.isPresent()) {
            Showtime showtime = showtimeOptional.get();

            // 날짜: 2025.07.21 (월) 형식
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
            String formattedDate = showtime.getStartTime().format(dateFormatter);

            // 시간: HH:mm 형식
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String formattedStartTime = showtime.getStartTime().format(timeFormatter);
            String formattedEndTime = showtime.getEndTime().format(timeFormatter);

            // 조조 할인 여부 판단 (10시 이전이면 true)
            boolean isMorning = showtime.getStartTime().getHour() < 10;


            // 예매된 좌석 정보 가져오기
            List<Reservation> reservations = reservationRepository.findByShowtime_ShowtimeId(showtimeId);

            List<String> reservedSeats = reservations.stream()
                    .map(r -> r.getSeatInfo())     // 예: "A3,A4"
                    .filter(Objects::nonNull)
                    .flatMap(info -> Arrays.stream(info.split(",")))
                    .collect(Collectors.toList());


            // 좌석 데이터 가져오기
            List<Seat> seats = seatRepository.findByScreen_ScreenId(showtime.getScreen().getScreenId());

            List<Map<String, Object>> seatRowList = seats.stream()
                    .collect(Collectors.groupingBy(
                            Seat::getSeatRow,
                            LinkedHashMap::new,
                            Collectors.mapping(seat -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("seatId", seat.getSeatId());
                                map.put("seatRow", seat.getSeatRow());
                                map.put("seatNumber", seat.getSeatNumber());
                                map.put("isVip", "VIP".equals(seat.getSeatType()));
                                map.put("isAccessible", "ACCESSIBLE".equals(seat.getSeatType()));
                                map.put("price", seat.getPrice());

                                String seatLabel = seat.getSeatRow() + seat.getSeatNumber();
                                map.put("isReserved", reservedSeats.contains(seatLabel));
                                map.put("isGap", false);
                                return map;
                            }, Collectors.toList())
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> rowMap = new HashMap<>();
                        rowMap.put("row", entry.getKey());
                        rowMap.put("seats", entry.getValue());
                        return rowMap;
                    })
                    .toList();

            // ageRating에 따른 이미지 경로 설정
            String ageRating = showtime.getMovie().getAgeRating(); // 예: "15"
            String ageImagePath = "/images/movie/" + ageRating + "_46x46.png";

            // 스크린 타입 가져오기 (IMAX, 4DX, 리클라이너 등등)
            String screenType = showtime.getScreen().getScreenType();

            // 모델에 담기
            model.addAttribute("showtime", showtime);
            model.addAttribute("movie", showtime.getMovie());
            model.addAttribute("screen", showtime.getScreen());
            model.addAttribute("formattedDate", formattedDate);
            model.addAttribute("formattedStartTime", formattedStartTime);
            model.addAttribute("formattedEndTime", formattedEndTime);
            model.addAttribute("isMorning", isMorning);
            model.addAttribute("seatRowList", seatRowList);
            model.addAttribute("title", "좌석 선택");
            model.addAttribute("ageImagePath", ageImagePath);
            model.addAttribute("screenType", showtime.getScreen().getScreenType());
            model.addAttribute("isStandard", "STANDARD".equals(screenType));
            model.addAttribute("isImax", "IMAX".equals(screenType));

            return "seats/seatReservation"; // 좌석 예약 페이지
        } else {
            return "redirect:/movies";  // 예매 정보가 없으면 영화 목록 페이지로 리디렉션
        }
    }
}
