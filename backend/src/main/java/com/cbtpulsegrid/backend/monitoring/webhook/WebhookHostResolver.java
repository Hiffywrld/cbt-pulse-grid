package com.cbtpulsegrid.backend.monitoring.webhook;

import java.net.InetAddress;
import java.net.UnknownHostException;

@FunctionalInterface
interface WebhookHostResolver {

	InetAddress[] resolve(String host) throws UnknownHostException;
}
