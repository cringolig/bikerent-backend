package com.company.bikerent.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks. Used for periodic cleanup of expired refresh tokens and
 * other maintenance tasks.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
