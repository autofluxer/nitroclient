package io.github.nitro.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Discovers other Nitro Client users on any multiplayer server.
 * Uses a public ntfy topic keyed by server address (tab-list presence only).
 */
public final class NitroPresence {

	private static final Logger LOGGER = LoggerFactory.getLogger("nitroclient");
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "nitro-tab-presence");
		t.setDaemon(true);
		return t;
	});
	private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
	private static final AtomicLong LAST_BEAT = new AtomicLong(0L);
	private static final AtomicLong LAST_POLL = new AtomicLong(0L);
	private static volatile String activeChannel = "";

	private NitroPresence() {
	}

	public static void tick(MinecraftClient client) {
		if (client == null || client.player == null || client.getNetworkHandler() == null) {
			stop();
			return;
		}
		NitroUsers.mark(client.player.getUuid());
		String channel = channelFor(client);
		if (channel == null || channel.isBlank()) {
			return;
		}
		activeChannel = channel;
		RUNNING.set(true);
		long now = System.currentTimeMillis();
		if (now - LAST_BEAT.get() > 20_000L) {
			LAST_BEAT.set(now);
			UUID id = client.player.getUuid();
			WORKER.execute(() -> heartbeat(channel, id));
		}
		if (now - LAST_POLL.get() > 12_000L) {
			LAST_POLL.set(now);
			WORKER.execute(() -> poll(channel));
		}
	}

	public static void stop() {
		RUNNING.set(false);
		activeChannel = "";
	}

	private static String channelFor(MinecraftClient client) {
		if (client.isInSingleplayer()) {
			return "nitro-tab-singleplayer";
		}
		ServerInfo info = client.getCurrentServerEntry();
		String address = info != null && info.address != null ? info.address.trim().toLowerCase() : "";
		if (address.isBlank()) {
			return null;
		}
		return "nitro-tab-" + shortHash(address);
	}

	private static void heartbeat(String channel, UUID uuid) {
		if (!RUNNING.get() || !channel.equals(activeChannel)) {
			return;
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) URI.create("https://ntfy.sh/" + channel).toURL().openConnection();
			conn.setConnectTimeout(4000);
			conn.setReadTimeout(4000);
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
			conn.setRequestProperty("Title", "nitro");
			conn.setRequestProperty("Tags", "nitro-client");
			byte[] body = ("nitro:" + uuid).getBytes(StandardCharsets.UTF_8);
			conn.setFixedLengthStreamingMode(body.length);
			try (OutputStream out = conn.getOutputStream()) {
				out.write(body);
			}
			int code = conn.getResponseCode();
			if (code >= 400) {
				LOGGER.debug("Nitro presence heartbeat HTTP {}", code);
			}
		} catch (Throwable t) {
			LOGGER.debug("Nitro presence heartbeat failed: {}", t.toString());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static void poll(String channel) {
		if (!RUNNING.get() || !channel.equals(activeChannel)) {
			return;
		}
		HttpURLConnection conn = null;
		try {
			String url = "https://ntfy.sh/" + URLEncoder.encode(channel, StandardCharsets.UTF_8)
					+ "/json?poll=1&since=30m";
			conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
			conn.setConnectTimeout(4000);
			conn.setReadTimeout(6000);
			conn.setRequestMethod("GET");
			int code = conn.getResponseCode();
			if (code >= 400) {
				return;
			}
			String raw;
			try (InputStream in = conn.getInputStream()) {
				raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			for (String line : raw.split("\n")) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				try {
					JsonElement el = JsonParser.parseString(line);
					if (!el.isJsonObject()) {
						continue;
					}
					JsonObject obj = el.getAsJsonObject();
					if (!obj.has("message")) {
						continue;
					}
					String message = obj.get("message").getAsString().trim();
					markFromMessage(message);
				} catch (Throwable ignored) {
				}
			}
		} catch (Throwable t) {
			LOGGER.debug("Nitro presence poll failed: {}", t.toString());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static void markFromMessage(String message) {
		if (message.startsWith("nitro:")) {
			message = message.substring(6).trim();
		}
		try {
			NitroUsers.mark(UUID.fromString(message));
		} catch (IllegalArgumentException ignored) {
		}
	}

	private static String shortHash(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest, 0, 8);
		} catch (Throwable t) {
			return Integer.toHexString(value.hashCode());
		}
	}
}
