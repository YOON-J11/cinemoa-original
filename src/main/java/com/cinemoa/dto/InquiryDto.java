package com.cinemoa.dto;

import com.cinemoa.entity.Inquiry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryDto {

    private Long inquiryId;           // 문의 고유번호
    private String memberId;          // 작성한 회원 ID
    private String title;             // 문의 제목
    private String content;           // 문의 내용
    private String replyContent;      // 관리자 답변 내용
    private String  regDate;    // 문의 등록일
    private String  replyDate;  // 답변 등록일
    private String status;

    public static InquiryDto fromEntity(Inquiry inquiry) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return InquiryDto.builder()
                .inquiryId(inquiry.getInquiryId())
                .memberId(inquiry.getMember() != null ? inquiry.getMember().getMemberId() : null)
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .replyContent(inquiry.getReplyContent())
                .regDate(inquiry.getRegDate() != null ? inquiry.getRegDate().format(formatter) : null)
                .replyDate(inquiry.getReplyDate() != null ? inquiry.getReplyDate().format(formatter) : null)
                .status(inquiry.getReplyContent() != null && !inquiry.getReplyContent().isBlank() ? "답변완료" : "대기중")
                .build();
    }


}
