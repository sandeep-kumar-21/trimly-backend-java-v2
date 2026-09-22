package com.trimly.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes database sequences and extensions required for collision-free Base62 URL short codes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Ensuring PostgreSQL sequence 'url_code_seq' exists...");
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS url_code_seq START WITH 100000 INCREMENT BY 1;");
            log.info("Database sequence 'url_code_seq' verified successfully.");
        } catch (Exception ex) {
            log.warn("Sequence initialization notice: {}", ex.getMessage());
        }
    }
}
