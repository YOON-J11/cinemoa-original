package com.cinemoa.service;

import com.cinemoa.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewEligibilityService {

    private final ReservationRepository reservationRepository;

    public boolean canWriteReview(String memberId, Long movieId) {
        if (memberId == null || movieId == null) return false;
        return reservationRepository.hasWatchedOrCompletedReservation(memberId, movieId);
    }


}
