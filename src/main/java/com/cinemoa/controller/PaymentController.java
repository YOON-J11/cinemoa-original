package com.cinemoa.controller;

import com.cinemoa.entity.*;
import com.cinemoa.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class PaymentController {

    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;
    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;

    @GetMapping("/payment")
    public String showPaymentPage(
            @RequestParam int amount,
            @RequestParam Long movieId,
            @RequestParam Long showtimeId,
            @RequestParam Long screenId,
            @RequestParam String seatInfo,
            @RequestParam List<Long> seatIdList,
            Model model,
            HttpSession session
    ) {
        // (선택) 로그인 세션 확인만 사용
        Object loginMember = session.getAttribute("loginMember");
        Object guestUser = session.getAttribute("guestUser");

        // ★ 로그인 사용자 이메일/이름 모델에 내려주기 (없으면 null)
        String buyerEmail = null;
        String buyerName = null;
        if (loginMember instanceof Member m) {
            buyerEmail = m.getEmail();
            buyerName = m.getName();
        }
        model.addAttribute("buyerEmail", buyerEmail);
        model.addAttribute("buyerName", buyerName);

        // showtime으로 상세 정보 조회해서 결제 페이지에도 표시
        Showtime showtime = showtimeRepository.findById(showtimeId).orElseThrow(() -> new IllegalArgumentException("Invalid showtimeId: " + showtimeId));


        Movie movie = showtime.getMovie();
        Screen screen = showtime.getScreen();
        Cinema cinema = screen.getCinema();

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");


        model.addAttribute("amount", amount);
        model.addAttribute("movieId", movieId);
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("screenId", screenId);
        model.addAttribute("seatInfo", seatInfo);
        model.addAttribute("seatIdList", seatIdList);

        // ★ 결제 페이지 표시용 상세 데이터
        model.addAttribute("movie", movie);
        model.addAttribute("screen", screen);
        model.addAttribute("cinema", cinema);
        model.addAttribute("formattedDate", showtime.getStartTime().format(dateFmt));
        model.addAttribute("formattedStartTime", showtime.getStartTime().format(timeFmt));
        model.addAttribute("formattedEndTime", showtime.getEndTime().format(timeFmt));
        model.addAttribute("isMorning", showtime.getStartTime().getHour() < 10);
        model.addAttribute("screenType", screen.getScreenType());
        model.addAttribute("isStandard", "STANDARD".equals(screen.getScreenType()));
        model.addAttribute("ageImagePath", "/images/movie/" + movie.getAgeRating() + "_46x46.png");


        return "payment/payment";
    }

    @PostMapping("/payment/complete")
    @ResponseBody
    @Transactional
    public ResponseEntity<String> completePayment(@RequestBody Map<String, Object> payload, HttpSession session) {
        try {
            // 1) 전달 값
            String impUid = (String) payload.get("impUid");
            int paidAmount = ((Number) payload.get("paidAmount")).intValue();
            String method = (String) payload.get("method");
            Long movieId = Long.valueOf(payload.get("movieId").toString());
            Long showtimeId = Long.valueOf(payload.get("showtimeId").toString());
            Long screenId = Long.valueOf(payload.get("screenId").toString());
            String seatInfo = (String) payload.get("seatInfo");

            @SuppressWarnings("unchecked")
            List<Object> rawSeatIds = (List<Object>) payload.get("seatIdList");
            List<Long> seatIds = rawSeatIds.stream().map(v -> Long.valueOf(v.toString())).collect(Collectors.toList());

            // 2) 아임포트 토큰 발급 (form-urlencoded)
            String impKey = "7713737840810560";   // TODO: 본인 계정 키로 교체
            String impSecret = "dMiYlmpKva4VtalV4HkOLqNK3P8KHshJ3ughJ9avTl8dS4qyn8KDTsRMxERIDrd4QaowezrtzsYHOfmw"; // TODO: 본인 시크릿

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.util.MultiValueMap<String, String> tokenForm =
                    new org.springframework.util.LinkedMultiValueMap<>();
            tokenForm.add("imp_key", impKey);
            tokenForm.add("imp_secret", impSecret);

            HttpEntity<org.springframework.util.MultiValueMap<String, String>> tokenHttpEntity =
                    new HttpEntity<>(tokenForm, tokenHeaders);

            ResponseEntity<JsonNode> tokenResp = restTemplate.postForEntity(
                    "https://api.iamport.kr/users/getToken",
                    tokenHttpEntity,
                    JsonNode.class
            );

            if (!tokenResp.getStatusCode().is2xxSuccessful()
                    || tokenResp.getBody() == null
                    || tokenResp.getBody().get("response") == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("토큰 발급 실패");
            }

            String accessToken = tokenResp.getBody().get("response").get("access_token").asText();

            // 3) 결제 조회 (Authorization 헤더만 사용)
            HttpHeaders payHeaders = new HttpHeaders();
            payHeaders.set("Authorization", accessToken);
            HttpEntity<Void> payInfoEntity = new HttpEntity<>(payHeaders);

            ResponseEntity<JsonNode> payResp = restTemplate.exchange(
                    "https://api.iamport.kr/payments/" + impUid,
                    HttpMethod.GET,
                    payInfoEntity,
                    JsonNode.class
            );

            int amountFromServer = payResp.getBody().get("response").get("amount").asInt();
            if (amountFromServer != paidAmount) {
                return ResponseEntity.badRequest().body("결제 금액 불일치");
            }

            // 4) 도메인 검증
            Showtime showtime = showtimeRepository.findById(showtimeId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid showtimeId: " + showtimeId));
            Cinema cinema = showtime.getScreen().getCinema();

            for (Long seatId : seatIds) {
                if (reservationSeatRepository.existsByShowtime_ShowtimeIdAndSeat_SeatId(showtimeId, seatId)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 예약된 좌석이 포함되어 있습니다.");
                }
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid seatId: " + seatId));
                if (!Objects.equals(seat.getScreen().getScreenId(), showtime.getScreen().getScreenId())) {
                    return ResponseEntity.badRequest().body("좌석이 상영관과 일치하지 않습니다. seatId=" + seatId);
                }
            }

            // 5) 예약 저장
            Reservation reservation = new Reservation();
            reservation.setReservationTime(LocalDateTime.now());
            reservation.setSeatInfo(seatInfo);
            reservation.setStatus("예약완료");
            reservation.setPaymentMethod(method);
            reservation.setCinema(cinema);
            reservation.setMovie(movieRepository.findById(movieId).orElse(null));
            reservation.setShowtime(showtime);
            reservation.setScreenId(screenId);

            Object loginMember = session.getAttribute("loginMember");
            if (loginMember instanceof Member) {
                reservation.setMember((Member) loginMember);
            }
            reservationRepository.save(reservation);

            // 6) 좌석 저장
            for (Long seatId : seatIds) {
                ReservationSeat rs = new ReservationSeat();
                rs.setReservation(reservation);
                rs.setSeat(seatRepository.findById(seatId).orElse(null));
                rs.setShowtime(showtime);
                reservationSeatRepository.save(rs);
            }

            // 7) 결제 저장
            Payment payment = new Payment();
            payment.setReservation(reservation);
            payment.setAmount(paidAmount);
            payment.setMethod(method);
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setTransactionId(impUid);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // 8) OK 응답
            return ResponseEntity.ok("OK:" + reservation.getReservationId());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 에러: " + e.getMessage());
        }
    }

    @GetMapping("/complete")
    public String reservationComplete(@RequestParam Long reservationId, Model model) {
        Reservation r = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));

        // 지연로딩 주의: 화면에서 필요한 것들 미리 꺼내두기
        Showtime showtime = r.getShowtime();
        Movie movie = r.getMovie();
        Cinema cinema = r.getCinema();
        Screen screen = showtime.getScreen();

        // 좌석 상세가 필요하면 reservation_seats 조회
        List<ReservationSeat> seats = reservationSeatRepository.findByReservation_ReservationId(reservationId);

        model.addAttribute("reservation", r);
        model.addAttribute("movie", movie);
        model.addAttribute("cinema", cinema);
        model.addAttribute("screen", screen);
        model.addAttribute("showtime", showtime);
        model.addAttribute("seatInfo", r.getSeatInfo());
        model.addAttribute("reservationSeats", seats);

        return "payment/paymentComplete";
    }

}
