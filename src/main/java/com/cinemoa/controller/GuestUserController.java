package com.cinemoa.controller;

import com.cinemoa.dto.GuestUserDto;
import com.cinemoa.entity.GuestUser;
import com.cinemoa.service.GuestUserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/guest")
public class GuestUserController {
    private final GuestUserService guestUserService;

    @GetMapping("/login")
    public String showGuestLoginForm() {
        return "member/guestLogin";
    }

    @PostMapping("/login")
    public String processGuestLogin(@ModelAttribute GuestUserDto dto, Model model, HttpSession session) {
        // 비밀번호 확인 일치 여부 검사
        if (!dto.getReservationPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("error", "예매 비밀번호가 일치하지 않습니다.");
            return "member/guestLogin";
        }

        // 로그인 또는 신규 등록
        GuestUser guest = guestUserService.login(dto);
        if (guest == null) {
            guest = guestUserService.register(dto);
        }

        // 세션에 비회원 로그인 저장
        session.setAttribute("guestUser", guest);
        return "redirect:/reservation/confirm"; // 결제 전 페이지 등으로 이동
    }
}
