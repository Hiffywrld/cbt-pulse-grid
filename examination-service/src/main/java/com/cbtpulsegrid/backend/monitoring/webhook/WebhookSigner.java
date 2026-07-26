package com.cbtpulsegrid.backend.monitoring.webhook;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
class WebhookSigner {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final String DERIVATION_CONTEXT = "cbt-pulse-grid:webhook:v1:";

	private final WebhookProperties properties;

	WebhookSigner(WebhookProperties properties) {
		this.properties = properties;
	}

	String deriveEncodedSecret(UUID subscriptionId, int secretVersion) {
		return Base64.getEncoder().encodeToString(deriveSecret(subscriptionId, secretVersion));
	}

	String sign(UUID subscriptionId, int secretVersion, String timestamp, byte[] body) {
		byte[] prefix = (timestamp + ".").getBytes(StandardCharsets.UTF_8);
		byte[] message = new byte[prefix.length + body.length];
		System.arraycopy(prefix, 0, message, 0, prefix.length);
		System.arraycopy(body, 0, message, prefix.length, body.length);
		return "v1=" + HexFormat.of().formatHex(hmac(deriveSecret(subscriptionId, secretVersion), message));
	}

	private byte[] deriveSecret(UUID subscriptionId, int secretVersion) {
		String context = DERIVATION_CONTEXT + subscriptionId + ":" + secretVersion;
		return hmac(
				properties.decodedMasterKey(),
				context.getBytes(StandardCharsets.UTF_8)
		);
	}

	private static byte[] hmac(byte[] key, byte[] message) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
			return mac.doFinal(message);
		}
		catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
		}
	}
}
