package com.cinemoa.config;

import com.cinemoa.entity.GuestUser;
import com.cinemoa.entity.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

    @ModelAttribute("loginMember")
    public Member getLoginMember(HttpSession session) {
        return (Member) session.getAttribute("loginMember");
    }

    @ModelAttribute("guestUser")
    public GuestUser getGuestUser(HttpSession session) { return (GuestUser) session.getAttribute("guestUser"); }

    @ModelAttribute("timestamp")
    public long globalTimestamp() {
        return System.currentTimeMillis();
    }


}
