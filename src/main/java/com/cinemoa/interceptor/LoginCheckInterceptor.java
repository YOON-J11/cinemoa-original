package com.cinemoa.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

//로그인 여부를 체크하는 인터셉터
public class LoginCheckInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        boolean isMemberLoggedIn = session != null && session.getAttribute("loginMember") != null;
        boolean isGuestLoggedIn = session != null && session.getAttribute("guestUser") != null;

        if (!isMemberLoggedIn && !isGuestLoggedIn) {
            response.sendRedirect("/member/login");
            return false;
        }

        return true;
    }
}

