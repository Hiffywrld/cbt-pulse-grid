package com.cbtpulsegrid.backend;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityServiceClockConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
