package io.github.nitro.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publishes this player's Nitro friends presence (username + current server)
 * so launcher friends can see Online / In game.
 */
public final class NitroFriendsPresence {

	private static final Logger LOGGER = LoggerFactory.getLogger("nitroclient");
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "nitro-friends-presence");
		t.setDaemon(true);
		return t;
	});
	private static final AtomicLong LAST_BEAT = new AtomicLong(0L);

	private NitroFriendsPresence() {
	}

	public static void tick(MinecraftClient client) {
		if (client == null || client.player == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - LAST_BEAT.get() < 20_000L) {
			return;
		}
		LAST_BEAT.set(now);
		String name = client.getSession() != null ? client.getSession().getUsername() : client.player.getName().getString();
		String server = currentServer(client);
		WORKER.execute(() -> heartbeat(name, server));
	}

	private static String currentServer(MinecraftClient client) {
		if (client.isInSingleplayer()) {
			return "Singleplayer";
		}
		ServerInfo info = client.getCurrentServerEntry();
		if (info != null && info.address != null && !info.address.isBlank()) {
			return info.address.trim();
		}
		return "";
	}

	private static void heartbeat(String name, String server) {
		if (name == null || name.length() < 3) {
			return;
		}
		try {
			String topic = "nfrp" + shortHash("nitro-friends:p:" + name.toLowerCase());
			String body = "{\"v\":1,\"t\":\"p\",\"n\":\"" + escape(name) + "\",\"s\":\"ingame\",\"sv\":\""
					+ escape(server) + "\",\"at\":" + System.currentTimeMillis() + "}";
			String[] hosts = { "ntfy.envs.net", "ntfy.adminforge.de" };
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			for (String host : hosts) {
				HttpURLConnection tryConn = null;
				try {
					tryConn = (HttpURLConnection) URI.create("https://" + host + "/" + topic).toURL().openConnection();
					tryConn.setConnectTimeout(3000);
					tryConn.setReadTimeout(3000);
					tryConn.setRequestMethod("POST");
					tryConn.setDoOutput(true);
					tryConn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
					tryConn.setFixedLengthStreamingMode(bytes.length);
					try (OutputStream out = tryConn.getOutputStream()) {
						out.write(bytes);
					}
					tryConn.getResponseCode();
				} catch (Throwable ignored) {
					// try next host
				} finally {
					if (tryConn != null) {
						tryConn.disconnect();
					}
				}
			}
		} catch (Throwable t) {
			LOGGER.debug("Nitro friends presence failed: {}", t.toString());
		}
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
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
