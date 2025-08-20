package com.cinemoa.service;

import com.cinemoa.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShowtimeWindowRefillService {

    private final ShowtimeRepository repo;

    @Transactional
    public int refillFutureWindow() {
        return repo.refillFutureWindow14dFromRecent3w();
    }
}
