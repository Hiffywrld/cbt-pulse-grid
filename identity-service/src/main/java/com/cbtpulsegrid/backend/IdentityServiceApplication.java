package com.cbtpulsegrid.backend;

import com.cbtpulsegrid.backend.identity.bootstrap.BootstrapAdminProperties;
import com.cbtpulsegrid.backend.identity.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(excludeFilters = @ComponentScan.Filter(
		type = FilterType.REGEX,
		pattern = "com\\.cbtpulsegrid\\.backend\\.audit\\.api\\..*"))
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, BootstrapAdminProperties.class})
public class IdentityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}
}