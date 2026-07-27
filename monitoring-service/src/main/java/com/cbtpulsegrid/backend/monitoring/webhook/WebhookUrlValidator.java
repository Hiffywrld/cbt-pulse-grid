package com.cbtpulsegrid.backend.monitoring.webhook;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
class WebhookUrlValidator {

	private final WebhookProperties properties;
	private final WebhookHostResolver hostResolver;

	WebhookUrlValidator(WebhookProperties properties, WebhookHostResolver hostResolver) {
		this.properties = properties;
		this.hostResolver = hostResolver;
	}

	URI validate(String destinationUrl) {
		URI uri;
		try {
			uri = new URI(destinationUrl == null ? "" : destinationUrl.trim());
		}
		catch (URISyntaxException exception) {
			throw new IllegalArgumentException("Webhook destination URL is invalid");
		}
		String scheme = uri.getScheme() == null
				? ""
				: uri.getScheme().toLowerCase(Locale.ROOT);
		if (properties.allowPrivateHttp()) {
			if (!scheme.equals("https") && !scheme.equals("http")) {
				throw new IllegalArgumentException("Webhook destination must use HTTP or HTTPS");
			}
		}
		else if (!scheme.equals("https")) {
			throw new IllegalArgumentException("Webhook destination must use HTTPS");
		}
		if (uri.getHost() == null || uri.getHost().isBlank()
				|| uri.getRawUserInfo() != null
				|| uri.getRawFragment() != null) {
			throw new IllegalArgumentException("Webhook destination URL is not allowed");
		}
		validatePort(uri);
		InetAddress[] addresses;
		try {
			addresses = hostResolver.resolve(uri.getHost());
		}
		catch (UnknownHostException exception) {
			throw new IllegalArgumentException("Webhook destination host cannot be resolved");
		}
		if (addresses.length == 0) {
			throw new IllegalArgumentException("Webhook destination host cannot be resolved");
		}
		if (!properties.allowPrivateHttp()) {
			for (InetAddress address : addresses) {
				if (!isPublicAddress(address)) {
					throw new IllegalArgumentException("Webhook destination address is not allowed");
				}
			}
		}
		return uri;
	}

	private void validatePort(URI uri) {
		int port = uri.getPort();
		if (port < -1 || port > 65535 || port == 0) {
			throw new IllegalArgumentException("Webhook destination port is invalid");
		}
		if (!properties.allowPrivateHttp() && port != -1 && port != 443) {
			throw new IllegalArgumentException("Webhook destination port is not allowed");
		}
	}

	private static boolean isPublicAddress(InetAddress address) {
		if (address.isAnyLocalAddress()
				|| address.isLoopbackAddress()
				|| address.isLinkLocalAddress()
				|| address.isSiteLocalAddress()
				|| address.isMulticastAddress()) {
			return false;
		}
		if (address instanceof Inet4Address) {
			return isPublicIpv4(address.getAddress());
		}
		if (address instanceof Inet6Address) {
			return isPublicIpv6(address.getAddress());
		}
		return false;
	}

	private static boolean isPublicIpv4(byte[] address) {
		int first = Byte.toUnsignedInt(address[0]);
		int second = Byte.toUnsignedInt(address[1]);
		int third = Byte.toUnsignedInt(address[2]);
		if (first == 0 || first == 10 || first == 127 || first >= 224) {
			return false;
		}
		if (first == 100 && second >= 64 && second <= 127) {
			return false;
		}
		if (first == 169 && second == 254) {
			return false;
		}
		if (first == 172 && second >= 16 && second <= 31) {
			return false;
		}
		if (first == 192 && (second == 168 || second == 0 || second == 2)) {
			return false;
		}
		if (first == 198 && (second == 18 || second == 19
				|| (second == 51 && third == 100))) {
			return false;
		}
		return !(first == 203 && second == 0 && third == 113);
	}

	private static boolean isPublicIpv6(byte[] address) {
		int first = Byte.toUnsignedInt(address[0]);
		int second = Byte.toUnsignedInt(address[1]);
		if ((first & 0xfe) == 0xfc) {
			return false;
		}
		return !(first == 0x20
				&& second == 0x01
				&& Byte.toUnsignedInt(address[2]) == 0x0d
				&& Byte.toUnsignedInt(address[3]) == 0xb8);
	}
}
