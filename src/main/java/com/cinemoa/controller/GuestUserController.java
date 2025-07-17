package com.cinemoa.controller;

import com.cinemoa.dto.GuestUserDto;
import com.cinemoa.entity.GuestUser;
import com.cinemoa.service.GuestUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/guest")
public class GuestUserController {
    private final GuestUserService guestUserService;

    @GetMapping("/login")
    public String showGuestLoginForm(@RequestParam(value = "redirect", required = false) String redirect, Model model) {
        model.addAttribute("redirect", redirect != null ? redirect : "");
        return "member/guestLogin";
    }

    @PostMapping("/login")
    public String processGuestLogin(@ModelAttribute GuestUserDto dto, @RequestParam(value = "redirect", required = false) String redirectUrl, Model model, HttpSession session) {
        // 비밀번호 확인 일치 여부 검사
        if (!dto.getReservationPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("error", "예매 비밀번호가 일치하지 않습니다.");
            model.addAttribute("redirect", redirectUrl);
            return "member/guestLogin";
        }

        // 로그인 또는 신규 등록
        GuestUser guest = guestUserService.login(dto);
        if (guest == null) {
            guest = guestUserService.register(dto);
        }

        // 세션에 비회원 로그인 저장
        session.setAttribute("guestUser", guest);

        // redirect 파라미터가 있으면 해당 경로로 이동, 없으면 홈으로
        if (redirectUrl == null || redirectUrl.trim().isEmpty()) {
            redirectUrl = "/";
        }
        return "redirect:" + redirectUrl;
    }
}
