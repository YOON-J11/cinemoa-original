package com.cinemoa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestUserDto {
    private String name;
    private LocalDate birthDate;
    private String phone;
    private String reservationPassword;
    private String confirmPassword;
}
