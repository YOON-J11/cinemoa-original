package com.cinemoa.config;

import com.cinemoa.service.ShowtimeWindowRefillService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ShowtimeWindowRefillConfig {
    private final ShowtimeWindowRefillService refillService;

    @Bean
    public ApplicationRunner showtimeWindowRefillRunner() {
        return args -> {
            refillService.refillFutureWindow();
        };
    }
}
