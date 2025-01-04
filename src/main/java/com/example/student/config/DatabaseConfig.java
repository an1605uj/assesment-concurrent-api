package com.example.student.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;

import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@EnableRetry
public class DatabaseConfig {
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    private static final Logger logger = Logger.getLogger(DatabaseConfig.class.getName());

    @Bean
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "fallbackForDatabase")
    public DataSource dataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);

            return new HikariDataSource(config);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to establish a database connection", ex);
            throw ex;
        }
    }
    public DataSource fallbackForDatabase(Exception ex) {
        logger.log(Level.SEVERE, "Max retry attempts or circuit breaker triggered. Falling back to default DataSource.", ex);
        return getDefaultDataSource();
    }

    private DataSource getDefaultDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        return new HikariDataSource(config);
    }
}
