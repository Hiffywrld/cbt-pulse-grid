package com.cbtpulsegrid.backend.attempt;

import java.security.SecureRandom;
import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class AttemptConfiguration {

	@Bean
	Clock attemptClock() {
		return Clock.systemUTC();
	}

	@Bean
	SecureRandom attemptSecureRandom() {
		return new SecureRandom();
	}
}
