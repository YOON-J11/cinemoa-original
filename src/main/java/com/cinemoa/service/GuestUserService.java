package com.cinemoa.service;

import com.cinemoa.dto.GuestUserDto;
import com.cinemoa.entity.GuestUser;
import com.cinemoa.repository.GuestUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class GuestUserService {
    private final GuestUserRepository guestUserRepository;

    public GuestUser login(GuestUserDto dto) {
        return guestUserRepository.findByNameAndBirthDateAndPhoneAndReservationPassword(
                dto.getName(), dto.getBirthDate(), dto.getPhone(), dto.getReservationPassword()
        ).orElse(null);
    }

    public GuestUser register(GuestUserDto dto) {
        GuestUser guest = GuestUser.builder()
                .name(dto.getName())
                .birthDate(dto.getBirthDate())
                .phone(dto.getPhone())
                .reservationPassword(dto.getReservationPassword())
                .build();
        return guestUserRepository.save(guest);
    }
}
