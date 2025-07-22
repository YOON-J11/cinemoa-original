package com.cinemoa.service;

import com.cinemoa.dto.*;
import com.cinemoa.entity.Member;
import com.cinemoa.entity.Payment;
import com.cinemoa.entity.Reservation;
import com.cinemoa.entity.Seat;
import com.cinemoa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class MemberService {

    private final MemberRepository memberRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    //회원가입
    public Member join(MemberDto dto) {
        Member member = Member.builder()
                .memberId(dto.getMemberId())
                .password(dto.getPassword())
                .name(dto.getName())
                .nickname(dto.getNickname())
                .birthDate(dto.getBirthDate())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .profileImg(dto.getProfileImg())
                .anniversary(dto.getAnniversary())
                .address(dto.getAddress())
                .preferredCinema(dto.getPreferredCinema())
                .preferredGenres(dto.getPreferredGenres())
                .build();

        return memberRepository.save(member);
    }

    //아이디 중복 검사
    public boolean isDuplicateId(String id) {
        return memberRepository.existsByMemberId(id);
    }

    //닉네임 중복 검사
    public boolean isDuplicateNickname(String nickname) {
        return memberRepository.existsByNickname(nickname);
    }

    //로그인 처리
    public Member login(String id, String password) {
        return memberRepository.findByMemberIdAndPassword(id, password);
    }

    //이메일 중복 확인 메서드
    public boolean isEmailDuplicated(String email) {
        return memberRepository.existsByEmail(email);
    }

    // 이름과 이메일로 회원 정보 조회 → 아이디/가입일 반환용
    public MemberDto findMemberId(String name, String email) {
        Member member = memberRepository.findByNameAndEmail(name, email).orElse(null);
        if (member != null) {
            // 필요한 정보만 담은 DTO 반환
            return MemberDto.builder()
                    .memberId(member.getMemberId())
                    .regDate(member.getRegDate().toLocalDate())
                    .build();
        }
        return null;
    }

    // 입력한 정보가 DB에 실제 존재하는지 검사하는 메서드
    public boolean validateMemberInfo(String memberId, String name, String email) {
        return memberRepository.findByMemberIdAndNameAndEmail(memberId, name, email).isPresent();
    }

    // 비밀번호 재설정 메서드
    public boolean updatePassword(String memberId, String newPassword) {
        Optional<Member> optionalMember = memberRepository.findByMemberId(memberId);
        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();
            member.setPassword(newPassword); // 필요시 암호화
            memberRepository.save(member);
            return true;
        }
        return false;
    }

    // 내가 본 영화 수
    public int countWatchedMovies(String memberId) {
        return memberRepository.countWatchedMovies(memberId);
    }

    // 내가 쓴 관람평 수
    public int countWrittenReviews(String memberId) {
        return memberRepository.countWrittenReviews(memberId);
    }

    // 최근 예매 내역 (5건)
    @Autowired
    private ReservationRepository reservationRepository;

    public List<ReservationDto> getRecentReservations(String memberId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return reservationRepository.findTop5ByMember_MemberIdOrderByReservationTimeDesc(memberId)
                .stream()
                .map(reservation -> ReservationDto.builder()
                        .reservationId(reservation.getReservationId())
                        .memberId(reservation.getMember().getMemberId())
                        .movieId(reservation.getMovie().getMovieId())
                        .cinemaId(reservation.getCinema().getCinemaId())
                        .screenId(reservation.getScreenId())
                        .seatInfo(reservation.getSeatInfo())
                        .reservationTime(reservation.getReservationTime())
                        .reservationTime(reservation.getReservationTime())
                        .paymentMethod(reservation.getPaymentMethod())
                        .status(reservation.getStatus())
                        .movieTitle(reservation.getMovie().getTitle())
                        .cinemaName(reservation.getCinema().getName())
                        .formattedReservationDate(reservation.getReservationTime().format(formatter))
                        .build()
                )
                .toList();
    }



    // 최근 문의 내역 (5건)
    public List<InquiryDto> getRecentInquiries(String memberId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return memberRepository.getRecentInquiries(memberId).stream()
                .limit(5)
                .map(inquiry -> InquiryDto.builder()
                        .inquiryId(inquiry.getInquiryId())
                        .title(inquiry.getTitle())
                        .regDate(inquiry.getRegDate().format(formatter)) // String으로 변환
                        .status(inquiry.getReplyContent() != null ? "답변 완료" : "미답변")
                        .build())
                .collect(Collectors.toList());
    }


    // 선호 영화관/장르 업데이트
    public void updatePreference(Member member) {
        memberRepository.save(member);
    }


    // 회원 탈퇴 처리
    @Transactional
    public void withdraw(Member member) {
        member.setDeleted(true);
        memberRepository.save(member);
    }

    public List<ReservationDto> getAllReservations(String memberId) {
        List<Reservation> reservations = reservationRepository.findByMember_MemberId(memberId);

        return reservations.stream()
                .map(reservation -> {
                    // 결제일 조회
                    Optional<Payment> paymentOpt = paymentRepository.findByReservation_ReservationId(reservation.getReservationId());

                    // 결제일 포맷
                    String formattedPaymentDate = paymentOpt
                            .map(payment -> payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm", Locale.KOREAN)))
                            .orElse("결제 정보 없음");
                    // 상영 시작 시간 포맷 (한글 요일)
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm", Locale.KOREAN);
                    String formattedShowtimeStart = reservation.getShowtime().getStartTime().format(formatter);

                    String seatTypeSummary = reservationSeatRepository
                            .findByReservation_ReservationId(reservation.getReservationId())
                            .stream()
                            .map(rs -> {
                                Seat seat = rs.getSeat();
                                String type = (seat != null && seat.getSeatType() != null)
                                        ? seat.getSeatType()
                                        : "알수없음";
                                return type;
                            })
                            .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                            .entrySet()
                            .stream()
                            .map(e -> e.getKey() + " " + e.getValue())
                            .collect(Collectors.joining(", "));

                    return ReservationDto.builder()
                            .reservationId(reservation.getReservationId())
                            .memberId(reservation.getMember().getMemberId())
                            .movieId(reservation.getMovie().getMovieId())
                            .cinemaId(reservation.getCinema().getCinemaId())
                            .screenId(reservation.getScreenId())
                            .seatInfo(reservation.getSeatInfo())
                            .seatTypeSummary(seatTypeSummary)
                            .reservationTime(reservation.getReservationTime())
                            .paymentMethod(reservation.getPaymentMethod())
                            .status(reservation.getStatus())
                            .movieTitle(reservation.getMovie().getTitle())
                            .cinemaName(reservation.getCinema().getName())
                            .mainImageUrl(reservation.getMovie().getMainImageUrl())
                            .formattedPaymentDate(formattedPaymentDate)
                            .formattedShowtimeStart(formattedShowtimeStart)
                            .build();
                })
                .collect(Collectors.toList());
    }


    public ReservationDetailDto getReservationDetail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 예매가 존재하지 않습니다."));

        // 좌석 정보 조회
        List<SeatDto> seatDtos = reservationSeatRepository.findByReservation_ReservationId(reservationId)
                .stream()
                .map(resSeat -> {
                    var seat = resSeat.getSeat();
                    return SeatDto.builder()
                            .seatId(seat.getSeatId())
                            .seatRow(seat.getSeatRow())
                            .seatNumber(seat.getSeatNumber())
                            .seatType(seat.getSeatType())
                            .price(seat.getPrice())
                            .build();
                })
                .toList();

        // 결제일 조회
        Optional<Payment> paymentOpt = paymentRepository.findByReservation_ReservationId(reservationId);
        String formattedPaymentDate = paymentOpt
                .map(payment -> payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm", Locale.KOREAN)))
                .orElse("결제 정보 없음");

        // 예매일 포맷 ( 달 Mon->월  로 한글표기)
        String formattedReservationTime = reservation.getReservationTime()
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd (E) HH:mm", Locale.KOREAN));


        // 총 가격 계산
        int totalPrice = seatDtos.stream().mapToInt(SeatDto::getPrice).sum();

        // 할인은 일단 0원으로 고정 (추후 로직 추가 가능)
        int discount = 0;
        int finalPayment = totalPrice - discount;

        // 상영 시작/종료 시간
        String showtimeStart = reservation.getShowtime().getStartTime().toString(); // 예: "15:30"
        String showtimeEnd = reservation.getShowtime().getEndTime().toString();     // 예: "17:30"

        // 판매번호: 예) 2025-0720-0001 (임의 규칙: 예매날짜 + 예약ID 네자리)
        String salesNumber = String.format("%s-%04d",
                reservation.getReservationTime().toLocalDate().toString().replace("-", ""),
                reservation.getReservationId());

        return ReservationDetailDto.builder()
                .reservationId(reservation.getReservationId())
                .movieTitle(reservation.getMovie().getTitle())
                .mainImageUrl(reservation.getMovie().getMainImageUrl())
                .cinemaName(reservation.getCinema().getName())
                .screenName(reservation.getScreen().getScreenName())
                .paymentMethod(reservation.getPaymentMethod())
                .status(reservation.getStatus())
                .reservationTime(reservation.getReservationTime())
                .formattedReservationTime(formattedReservationTime)
                .showtimeStart(showtimeStart)
                .showtimeEnd(showtimeEnd)
                .seats(seatDtos)
                .totalPrice(totalPrice)
                .discount(discount)
                .finalPayment(finalPayment)
                .salesNumber(salesNumber)
                .formattedPaymentDate(formattedPaymentDate)
                .build();
    }

    //내가 본 영화
    public List<WatchedMovieDto> getWatchedMovies(String memberId) {
        List<Reservation> reservations = reservationRepository.findByMember_MemberIdAndStatus(memberId, "예약완료");

        return reservations.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() ->
                                new TreeSet<Reservation>(
                                        Comparator.comparing((Reservation r) ->
                                                r.getMovie().getMovieId() + "_" + r.getShowtime().getShowtimeId()
                                        )
                                )
                        ),
                        ArrayList::new
                ))
                .stream()
                .map(reservation -> {
                    var movie = reservation.getMovie();
                    var cinema = reservation.getCinema();
                    var screen = reservation.getScreen();
                    var showtime = reservation.getShowtime();

                    // 날짜 및 시간 포맷
                    var startTime = showtime.getStartTime();
                    var endTime = showtime.getEndTime();
                    String date = startTime.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                    String day = getKoreanDayOfWeek(startTime.getDayOfWeek());
                    String time = startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "~" +
                            endTime.format(DateTimeFormatter.ofPattern("HH:mm"));

                    // 관람 인원 수
                    int personCount = reservation.getSeatInfo() != null
                            ? reservation.getSeatInfo().split(",").length
                            : 0;

                    // 관람평 조회
                    var reviewOpt = reviewRepository.findByMovie_MovieIdAndUser_MemberId(movie.getMovieId(), memberId);
                    boolean hasReview = reviewOpt.isPresent();
                    String reviewContent = hasReview ? reviewOpt.get().getContent() : null;

                    return WatchedMovieDto.builder()
                            .movieId(movie.getMovieId()) // 리뷰 작성 링크에 필요
                            .title(movie.getTitle())
                            .poster(movie.getMainImageUrl())
                            .grade(movie.getAgeRating())
                            .date(date)
                            .day("(" + day + ")")
                            .time(time)
                            .cinemaName(cinema.getName())
                            .screenName(screen.getScreenName())
                            .seatType(screen.getScreenType())
                            .personCount(personCount)
                            .hasReview(hasReview)
                            .reviewContent(reviewContent)
                            .build();

                }).collect(Collectors.toList());
    }


    //요일 변환 메서드
    private String getKoreanDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }


    public List<WatchedMovieDto> getReviewedMovies(String memberId) {
        List<WatchedMovieDto> allWatchedMovies = getWatchedMovies(memberId);

        // 관람평이 작성된 영화만 필터링
        return allWatchedMovies.stream()
                .filter(WatchedMovieDto::isHasReview) // hasReview == true 인 경우만
                .collect(Collectors.toList());
    }


}
