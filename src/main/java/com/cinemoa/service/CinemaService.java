package com.cinemoa.service;

import com.cinemoa.entity.Cinema;
import com.cinemoa.repository.CinemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CinemaService {

    private final CinemaRepository cinemaRepository;

    public List<Cinema> getAllCinemas() {
        return cinemaRepository.findAllByOrderByCinemaIdAsc();
    }

    public List<Cinema> getCinemasByRegion(String region) {
        return cinemaRepository.findByRegion(region);
    }

    public String getCinemaNameById(Long cinemaId) {
        return cinemaRepository.findById(cinemaId)
                .map(Cinema::getName)
                .orElse("알 수 없음");
    }
}
