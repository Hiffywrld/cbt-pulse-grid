package com.cbtpulsegrid.backend.monitoring;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
@EnableConfigurationProperties({
		MonitoringWebSocketProperties.class,
		MissedHeartbeatProperties.class
})
class MonitoringWebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

	private final MonitoringWebSocketProperties properties;
	private final MonitoringStompSecurityInterceptor securityInterceptor;

	MonitoringWebSocketConfiguration(
			MonitoringWebSocketProperties properties,
			MonitoringStompSecurityInterceptor securityInterceptor
	) {
		this.properties = properties;
		this.securityInterceptor = securityInterceptor;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.setAllowedOrigins(properties.allowedOrigins().toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic");
		registry.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(securityInterceptor);
	}
}
