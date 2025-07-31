package com.cinemoa.repository;

import com.cinemoa.entity.Inquiry;
import com.cinemoa.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByMember(Member member);

    List<Inquiry> findByMember_MemberId(String memberId);

    List<Inquiry> findByGuestUser_GuestUserId(Long guestUserId);

    Page<Inquiry> findByMember_MemberId(String memberId, Pageable pageable);

    Page<Inquiry> findByGuestUser_GuestUserId(Long guestUserId, Pageable pageable);

}
