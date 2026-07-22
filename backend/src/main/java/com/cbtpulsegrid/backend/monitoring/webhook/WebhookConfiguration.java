package com.cbtpulsegrid.backend.monitoring.webhook;

import java.net.http.HttpClient;
import java.net.InetAddress;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebhookProperties.class)
class WebhookConfiguration {

	@Bean
	HttpClient webhookHttpClient(WebhookProperties properties) {
		return HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	@Bean
	WebhookHostResolver webhookHostResolver() {
		return InetAddress::getAllByName;
	}
}
