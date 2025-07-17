package com.cinemoa.controller;

import com.cinemoa.dto.MemberDto;
import com.cinemoa.entity.Member;
import com.cinemoa.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    // STEP1: 이메일 인증 페이지 (GET)
    @GetMapping("/join/step1")
    public String showJoinStep1(Model model) {
        return "member/joinStep1";
    }
    // 이메일 중복 확인
    @PostMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean duplicated = memberService.isEmailDuplicated(email);
        return ResponseEntity.ok(!duplicated); // 중복이면 false 반환
    }

    // STEP2: 약관 동의 페이지 (GET)
    @GetMapping("/join/step2")
    public String showStep2Page(HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute("emailVerified");
        if (verified == null || !verified) {
            return "redirect:/member/join/step1?error=unauthorized";
        }
        return "member/joinStep2";
    }

    @PostMapping("/join/step2/process")
    public String processStep2(@RequestParam(value = "terms", required = false) String terms,
                               @RequestParam(value = "privacy", required = false) String privacy) {
        // 약관 동의 확인
        if (terms == null || privacy == null) {
            // 하나라도 동의 안했으면 다시 step2로
            return "redirect:/member/join/step2?error=agree";
        }
        // 둘 다 동의했으면 step3으로 이동
        return "redirect:/member/join/step3";
    }

    // STEP3: 회원정보 입력 페이지 (GET)
    @GetMapping("/join/step3")
    public String showJoinStep3(HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute("emailVerified");
        if (verified == null || !verified) {
            return "redirect:/member/join/step1?error=unauthorized";
        }
        return "member/joinStep3";
    }
    @PostMapping("/join/step3/process")
    public String processStep3(@ModelAttribute MemberDto dto, HttpSession session, Model model) {
        // 이메일은 세션에서 가져오기 (step1에서 저장된 verifiedEmail)
        String verifiedEmail = (String) session.getAttribute("verifiedEmail");
        if (verifiedEmail == null) {
            return "redirect:/member/join/step1?error=auth";
        }
        dto.setEmail(verifiedEmail);

        // 아이디, 닉네임 중복 체크
        if (memberService.isDuplicateId(dto.getMemberId())) {
            model.addAttribute("error", "이미 사용 중인 아이디입니다.");
            return "member/joinStep3";
        }
        if (memberService.isDuplicateNickname(dto.getNickname())) {
            model.addAttribute("error", "이미 사용 중인 닉네임입니다.");
            return "member/joinStep3";
        }

        // 회원 가입 처리
        memberService.join(dto);

        // 로그인 세션 생성 (회원 가입 완료시 자동로그인)
        Member member = memberService.join(dto);
        session.setAttribute("loginMember", member); // 로그인 세션


        // 세션 정리 (선택사항)
        session.removeAttribute("verifiedEmail");
        session.removeAttribute("emailVerified");
        session.removeAttribute("authCode");
        session.removeAttribute("authEmail");

        // 가입 완료 페이지로 이동
        return "redirect:/member/join/step4";
    }

    // STEP4: 가입완료 페이지 (GET)
    @GetMapping("/join/step4")
    public String showJoinStep4(HttpSession session) {

        // 회원가입 과정에서 사용된 임시 세션 정보 제거
        session.removeAttribute("verifiedEmail");
        session.removeAttribute("emailVerified");
        session.removeAttribute("authCode");
        session.removeAttribute("authEmail");

        return "member/joinStep4";
    }


    //로그인 페이지 (GET)
    @GetMapping("/login")
    public String showLoginForm() {
    return "member/login";
    }

    //로그인 처리 (POST)
    @PostMapping("/login")
    public String processLogin(@RequestParam String memberId, @RequestParam String password, Model model, HttpSession session) {
        Member member = memberService.login(memberId, password);
        if (member == null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "member/login";
        }

        // 세션에 로그인 사용자 저장
        session.setAttribute("loginMember", member);

        // 이전에 저장된 리디렉션 경로가 있다면
        String redirectPath = (String) session.getAttribute("redirectAfterLogin");
        if (redirectPath != null) {
            session.removeAttribute("redirectAfterLogin");
            return "redirect:" + redirectPath;
        }

        return "redirect:/"; //로그인 성공 후 메인페이지로 이동
    }

    //로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 세션 초기화
        return "redirect:/"; // 홈으로 이동
    }
    //비회원로그아웃
    @GetMapping("/guest/logout")
    public String guestLogout(HttpSession session) {
        session.invalidate(); // 비회원도 전체 세션 삭제
        return "redirect:/";
    }

    // 아이디, 비밀번호 찾기 페이지
    @GetMapping("/find")
    public String showFindForm(Model model) {
        return "member/find";
    }


    // 아이디 찾기
    @PostMapping("/find-id")
    @ResponseBody
    public ResponseEntity<MemberDto> findId(@RequestParam String name, @RequestParam String email) {
        MemberDto result = memberService.findMemberId(name, email);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //선호관람정보 설정 메소드
    @PostMapping("/information/pref/update")
    public String updatePreference(
            @RequestParam(value = "preferredCinema", required = false) String preferredCinema,
            @RequestParam(value = "preferredGenres[]", required = false) List<String> preferredGenres,
            HttpSession session) {

        Member loginMember = (Member) session.getAttribute("loginMember");

        // 빈 문자열로 들어오면 null로 변환
        if (preferredCinema != null && preferredCinema.isBlank()) {
            preferredCinema = null;
        }

        // 장르 리스트 → 쉼표로 연결된 문자열 (null일 경우 빈 문자열 저장 또는 null 처리)
        String genresAsString = (preferredGenres != null && !preferredGenres.isEmpty())
                ? String.join(",", preferredGenres)
                : null;

        loginMember.setPreferredCinema(preferredCinema);
        loginMember.setPreferredGenres(genresAsString);

        // DB 저장
        memberService.updatePreference(loginMember);

        // 세션 갱신
        session.setAttribute("loginMember", loginMember);

        // 리다이렉트
        return "redirect:/mypage/information/pref";
    }



}
