package com.cbtpulsegrid.backend.monitoring.webhook.api;

import com.cbtpulsegrid.backend.monitoring.webhook.WebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebhookControllerRegistrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withBean(WebhookService.class, () -> mock(WebhookService.class))
			.withPropertyValues("app.webhooks.enabled=false")
			.withUserConfiguration(WebhookControllerScanConfiguration.class);

	@Test
	void registersManagementControllerWhenWebhookDeliveryIsDisabled() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(WebhookController.class));
	}

	@Configuration(proxyBeanMethods = false)
	@ComponentScan(basePackageClasses = WebhookController.class)
	static class WebhookControllerScanConfiguration {
	}
}
