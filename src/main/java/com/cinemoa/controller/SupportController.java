package com.cinemoa.controller;

import com.cinemoa.dto.FaqDto;
import com.cinemoa.dto.InquiryDto;
import com.cinemoa.dto.NoticeDto;
import com.cinemoa.entity.GuestUser;
import com.cinemoa.entity.Inquiry;
import com.cinemoa.entity.Member;
import com.cinemoa.service.FaqService;
import com.cinemoa.service.InquiryService;
import com.cinemoa.service.NoticeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
@RequestMapping("/support")
public class SupportController {

    private final InquiryService inquiryService;
    private final NoticeService noticeService;
    private final FaqService faqService;


    // 로그인 유저 세션 정보 추가 (공통 처리)
    private void addLoginMember(Model model, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember != null) {
            model.addAttribute("loginMember", loginMember);
        }
    }

    @GetMapping
    public String supportHome(Model model, HttpSession session) {
        model.addAttribute("supportHome", true);
        model.addAttribute("pagePath", "고객센터 홈");
        addLoginMember(model, session);
        return "support/supportLayout";
    }

    @GetMapping("/notice")
    public String noticeList(Model model, HttpSession session,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "page", defaultValue = "1") int page) {

        Page<NoticeDto> notices = noticeService.getNoticeList(keyword, page);
        int currentPage = notices.getNumber() + 1;
        int totalPages = notices.getTotalPages();

        int start = Math.max(currentPage - 2, 1);
        int end = Math.min(start + 4, totalPages);

        List<Map<String, Object>> pages = IntStream.rangeClosed(start, end)
                .mapToObj(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("pageNumber", i);
                    map.put("isActive", i == currentPage);
                    return map;
                }).toList();

        model.addAttribute("notices", notices);
        model.addAttribute("pages", pages);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("nextPage", notices.getNumber() + 2);
        model.addAttribute("noticeList", true);
        model.addAttribute("pagePath", "고객센터 > 공지사항");
        addLoginMember(model, session);
        return "support/supportLayout";
    }



    @GetMapping("/notice/{id}")
    public String noticeDetail(@PathVariable Long id, Model model, HttpSession session) {
        model.addAttribute("noticeDetail", true);
        model.addAttribute("pagePath", "고객센터 > 공지사항");
        addLoginMember(model, session);
        // id는 나중에 서비스에서 조회용으로 사용
        return "support/supportLayout";
    }

    @GetMapping("/faq")
    public String faqList(Model model, HttpSession session,
                          @RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "page", defaultValue = "1") int page) {

        Page<FaqDto> faqs = faqService.searchFaqs(keyword, page);
        int currentPage = faqs.getNumber() + 1;
        int totalPages = faqs.getTotalPages();

        int start = Math.max(currentPage - 2, 1);
        int end = Math.min(start + 4, totalPages);

        List<Map<String, Object>> pages = IntStream.rangeClosed(start, end)
                .mapToObj(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("pageNumber", i);
                    map.put("isActive", i == currentPage);
                    return map;
                }).toList();

        model.addAttribute("faqs", faqs);
        model.addAttribute("pages", pages);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("nextPage", faqs.getNumber() + 2);
        model.addAttribute("faqList", true);
        model.addAttribute("pagePath", "고객센터 > 자주 묻는 질문");
        addLoginMember(model, session);
        return "support/supportLayout";
    }



    // 1:1 문의하기
    @GetMapping("/inquiryForm")
    public String inquiryForm(Model model, HttpSession session) {

        Member loginMember = (Member) session.getAttribute("loginMember");
        GuestUser guestUser = (GuestUser) session.getAttribute("guestUser");

        // 로그인 사용자 또는 비회원이 아니면 접근 불가
        if (loginMember == null && guestUser == null) {
            return "redirect:/member/login";
        }

        model.addAttribute("inquiryForm", true);
        model.addAttribute("pagePath", "고객센터 > 문의 작성");
        addLoginMember(model, session);
        return "support/supportLayout";
    }
    @PostMapping("/inquiryForm")
    public String submitInquiry(@RequestParam String category,
                                @RequestParam String title,
                                @RequestParam String content,
                                @RequestParam(required = false) String agree,
                                HttpSession session) {
        if (agree == null) {
            return "redirect:/support/inquiryForm?error=agree";
        }

        Member loginMember = (Member) session.getAttribute("loginMember");
        GuestUser guestUser = (GuestUser) session.getAttribute("guestUser"); // ★ 이거 꼭 추가

        if (loginMember == null && guestUser == null) {
            return "redirect:/member/login";
        }

        String finalTitle = "[" + category + "] " + title;

        Inquiry inquiry = Inquiry.builder()
                .title(finalTitle)
                .content(content)
                .regDate(LocalDateTime.now())
                .build();

        if (loginMember != null) {
            inquiry.setMember(loginMember);
        } else {
            inquiry.setGuestUser(guestUser);
        }

        inquiryService.saveInquiry(inquiry);
        return "redirect:/support/myinquiry";
    }


    @GetMapping("/myinquiry")
    public String myinquiry(Model model, HttpSession session,
                            @RequestParam(defaultValue = "0") int page) {

        Member loginMember = (Member) session.getAttribute("loginMember");
        GuestUser guestUser = (GuestUser) session.getAttribute("guestUser");

        if (loginMember == null && guestUser == null) {
            return "redirect:/member/login";
        }

        Page<InquiryDto> inquiryPage = null;

        if (loginMember != null) {
            inquiryPage = inquiryService.getMyInquiries(loginMember.getMemberId(), page);
        } else {
            inquiryPage = inquiryService.getMyInquiriesForGuest(guestUser.getGuestUserId(), page);
        }

        int totalPages = inquiryPage.getTotalPages();
        List<Map<String, Object>> pageNumbers = IntStream.range(0, totalPages)
                .mapToObj(i -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("num", i); // 내부 링크용 (0부터 시작)
                    map.put("displayNum", i + 1); // 사용자에게 보일 숫자 (1부터 시작)
                    map.put("isCurrent", i == page);
                    return map;
                }).toList();



        model.addAttribute("inquiryPage", inquiryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("prevPage", page > 0 ? page - 1 : 0);
        model.addAttribute("nextPage", page + 1);

        model.addAttribute("myinquiry", true);
        model.addAttribute("pagePath", "고객센터 > 나의 문의 내역");
        addLoginMember(model, session);
        return "support/supportLayout";
    }


    @GetMapping("/inquiry/{id}")
    public String inquiryDetail(@PathVariable Long id, Model model, HttpSession session) {

        Member loginMember = (Member) session.getAttribute("loginMember");
        GuestUser guestUser = (GuestUser) session.getAttribute("guestUser");

        // 로그인 사용자 또는 비회원이 아니면 접근 불가
        if (loginMember == null && guestUser == null) {
            return "redirect:/member/login";
        }

        model.addAttribute("inquiryDetail", true);
        model.addAttribute("pagePath", "고객센터 > 나의 문의 내역");
        addLoginMember(model, session);
        return "support/supportLayout";
    }
}
