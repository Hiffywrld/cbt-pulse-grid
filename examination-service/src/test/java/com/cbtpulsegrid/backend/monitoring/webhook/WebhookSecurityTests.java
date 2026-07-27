package com.cbtpulsegrid.backend.monitoring.webhook;

import java.net.http.HttpClient;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookSecurityTests {

	private static final byte[] MASTER_KEY = "0123456789abcdef0123456789abcdef"
			.getBytes(StandardCharsets.UTF_8);

	@Test
	void derivesVersionedSecretsAndCalculatesExactSignature() throws Exception {
		WebhookSigner signer = new WebhookSigner(properties(false));
		UUID subscriptionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		byte[] body = "{\"payloadVersion\":\"1\"}".getBytes(StandardCharsets.UTF_8);
		String timestamp = "1893456000";

		byte[] secret = hmac(
				MASTER_KEY,
				("cbt-pulse-grid:webhook:v1:" + subscriptionId + ":1")
						.getBytes(StandardCharsets.UTF_8)
		);
		String expected = "v1=" + HexFormat.of().formatHex(hmac(
				secret,
				(timestamp + "." + new String(body, StandardCharsets.UTF_8))
						.getBytes(StandardCharsets.UTF_8)
		));

		assertEquals(Base64.getEncoder().encodeToString(secret), signer.deriveEncodedSecret(
				subscriptionId,
				1
		));
		assertEquals(expected, signer.sign(subscriptionId, 1, timestamp, body));
		org.junit.jupiter.api.Assertions.assertNotEquals(
				signer.deriveEncodedSecret(subscriptionId, 1),
				signer.deriveEncodedSecret(subscriptionId, 2)
		);
	}

	@Test
	void enabledConfigurationRequiresExactlyThirtyTwoBase64DecodedBytes() {
		assertThrows(IllegalArgumentException.class, () -> propertiesWithKey("not-base64"));
		assertThrows(IllegalArgumentException.class, () -> propertiesWithKey(
				Base64.getEncoder().encodeToString(new byte[31])
		));
		assertDoesNotThrow(() -> properties(false));
	}

	@Test
	void rejectsUnsafeProductionDestinationsAndAllowsPublicHttps() throws Exception {
		WebhookHostResolver publicResolver = host -> new InetAddress[]{
				InetAddress.getByAddress(new byte[]{8, 8, 8, 8})
		};
		WebhookUrlValidator validator = new WebhookUrlValidator(
				properties(false),
				publicResolver
		);

		assertEquals(
				"https://receiver.example/hooks/monitoring",
				validator.validate("https://receiver.example/hooks/monitoring").toString()
		);
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				"http://receiver.example/hook"
		));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				"https://user:password@receiver.example/hook"
		));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				"https://receiver.example:8443/hook"
		));
		assertThrows(IllegalArgumentException.class, () -> validator.validate(
				"https://receiver.example/hook#fragment"
		));
	}

	@Test
	void rejectsPrivateAndReservedDnsResultsUnlessDevelopmentSwitchIsEnabled() throws Exception {
		WebhookHostResolver privateResolver = host -> new InetAddress[]{
				InetAddress.getByAddress(new byte[]{127, 0, 0, 1})
		};
		WebhookUrlValidator production = new WebhookUrlValidator(
				properties(false),
				privateResolver
		);
		WebhookUrlValidator development = new WebhookUrlValidator(
				properties(true),
				privateResolver
		);

		assertThrows(IllegalArgumentException.class, () -> production.validate(
				"https://localhost/hook"
		));
		assertEquals(
				"http://localhost:9090/hook",
				development.validate("http://localhost:9090/hook").toString()
		);
	}

	@Test
	void javaHttpClientDisablesRedirectFollowing() {
		HttpClient client = new WebhookConfiguration().webhookHttpClient(properties(false));

		assertEquals(HttpClient.Redirect.NEVER, client.followRedirects());
	}

	static WebhookProperties properties(boolean allowPrivateHttp) {
		return propertiesWithKey(
				Base64.getEncoder().encodeToString(MASTER_KEY),
				allowPrivateHttp
		);
	}

	private static WebhookProperties propertiesWithKey(String masterKey) {
		return propertiesWithKey(masterKey, false);
	}

	private static WebhookProperties propertiesWithKey(
			String masterKey,
			boolean allowPrivateHttp
	) {
		return new WebhookProperties(
				true,
				masterKey,
				allowPrivateHttp,
				Duration.ofSeconds(3),
				Duration.ofSeconds(5),
				Duration.ofSeconds(5),
				Duration.ofSeconds(30),
				Duration.ofSeconds(10),
				Duration.ofMinutes(15),
				25,
				8
		);
	}

	private static byte[] hmac(byte[] key, byte[] value) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		return mac.doFinal(value);
	}
}
