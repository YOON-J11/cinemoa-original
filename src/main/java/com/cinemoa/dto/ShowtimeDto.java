
package com.cinemoa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeDto {
    private Long showtimeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime; // 계산된 종료 시간을 담을 필드
    private String screenName; // 상영관 이름
    private Long screenId;
}