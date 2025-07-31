package com.cinemoa.service;

import com.cinemoa.dto.InquiryDto;
import com.cinemoa.entity.Inquiry;
import com.cinemoa.entity.Member;
import com.cinemoa.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public void saveInquiry(Inquiry inquiry) {
        inquiryRepository.save(inquiry);
    }

    public List<Inquiry> getMyInquiries(Member member) {
        return inquiryRepository.findByMember(member);
    }

    // 회원용 문의 목록
    public List<InquiryDto> getMyInquiries(String memberId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return inquiryRepository.findByMember_MemberId(memberId).stream()
                .map(inquiry -> InquiryDto.builder()
                        .inquiryId(inquiry.getInquiryId())
                        .title(inquiry.getTitle())
                        .content(inquiry.getContent())
                        .replyContent(inquiry.getReplyContent())
                        .regDate(inquiry.getRegDate() != null ? inquiry.getRegDate().format(formatter) : null)
                        .replyDate(inquiry.getReplyDate() != null ? inquiry.getReplyDate().format(formatter) : null)
                        .build())
                .collect(Collectors.toList());
    }

    // 게스트용 문의 목록
    public List<InquiryDto> getMyInquiriesForGuest(Long guestUserId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return inquiryRepository.findByGuestUser_GuestUserId(guestUserId).stream()
                .map(inquiry -> InquiryDto.builder()
                        .inquiryId(inquiry.getInquiryId())
                        .title(inquiry.getTitle())
                        .content(inquiry.getContent())
                        .replyContent(inquiry.getReplyContent())
                        .regDate(inquiry.getRegDate() != null ? inquiry.getRegDate().format(formatter) : null)
                        .replyDate(inquiry.getReplyDate() != null ? inquiry.getReplyDate().format(formatter) : null)
                        .build())
                .collect(Collectors.toList());
    }


    public Page<InquiryDto> getMyInquiries(String memberId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("regDate").descending());
        Page<Inquiry> inquiries = inquiryRepository.findByMember_MemberId(memberId, pageable);
        return inquiries.map(InquiryDto::fromEntity);
    }

    public Page<InquiryDto> getMyInquiriesForGuest(Long guestUserId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("regDate").descending());
        Page<Inquiry> inquiries = inquiryRepository.findByGuestUser_GuestUserId(guestUserId, pageable);
        return inquiries.map(InquiryDto::fromEntity);
    }


}
