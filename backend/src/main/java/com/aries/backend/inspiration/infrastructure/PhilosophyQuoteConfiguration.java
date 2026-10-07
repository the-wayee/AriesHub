package com.aries.backend.inspiration.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PhilosophyQuoteProperties.class)
public class PhilosophyQuoteConfiguration {}
