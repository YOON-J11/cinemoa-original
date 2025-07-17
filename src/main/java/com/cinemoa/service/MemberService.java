package com.cinemoa.service;

import com.cinemoa.dto.InquiryDto;
import com.cinemoa.dto.MemberDto;
import com.cinemoa.dto.ReservationDto;
import com.cinemoa.entity.Member;
import com.cinemoa.repository.MemberRepository;
import com.cinemoa.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor // final 필드 생성자 자동 생성
public class MemberService {

    private final MemberRepository memberRepository;

    //회원가입
    public Member join(MemberDto dto){
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
    public boolean isDuplicateId(String id){
        return memberRepository.existsByMemberId(id);
    }

    //닉네임 중복 검사
    public boolean isDuplicateNickname(String nickname){
        return memberRepository.existsByNickname(nickname);
    }

    //로그인 처리
    public Member login(String id, String password){
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
        return reservationRepository.findTop5ByMember_MemberIdOrderByReservationTimeDesc(memberId)
                .stream()
                .map(reservation -> ReservationDto.builder()
                        .reservationId(reservation.getReservationId())
                        .memberId(reservation.getMember().getMemberId())
                        .movieId(reservation.getMovie().getMovieId())
                        .cinemaId(reservation.getCinema().getCinemaId())
                        .screenId(reservation.getScreenId())
                        .seatInfo(reservation.getSeatInfo())
                        .reservationTime(reservation.getReservationTime()) // LocalDateTime 유지
                        .paymentMethod(reservation.getPaymentMethod())
                        .status(reservation.getStatus())
                        .movieTitle(reservation.getMovie().getTitle())
                        .cinemaName(reservation.getCinema().getName())
                        .build()
                )
                .toList();
    }



    // 최근 문의 내역 (5건)
    public List<InquiryDto> getRecentInquiries(String memberId) {
        return memberRepository.getRecentInquiries(memberId);
    }

    // 선호 영화관/장르 업데이트
    public void updatePreference(Member member) {
        memberRepository.save(member);
    }

}
