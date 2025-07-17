package com.cinemoa.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PaymentComtroller {

    @GetMapping("/reservation/payment")
    public String showPaymentPage(HttpSession session) {
        // 로그인 여부 또는 세션 값 확인
        Object loginMember = session.getAttribute("loginMember");
        Object guestUser = session.getAttribute("guestUser");

        return "payment/payment";
    }

}
