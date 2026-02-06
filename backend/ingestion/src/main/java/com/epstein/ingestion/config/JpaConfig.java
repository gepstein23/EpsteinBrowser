package com.epstein.ingestion.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"com.epstein.common.model"})
@EnableJpaRepositories(basePackages = {"com.epstein.common.repository"})
public class JpaConfig {
}
