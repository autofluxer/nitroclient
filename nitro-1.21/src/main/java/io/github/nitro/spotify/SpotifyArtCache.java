package io.github.nitro.spotify;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads album art off-thread, caches on disk, registers textures on the render thread.
 */
public final class SpotifyArtCache {

	private static final Logger LOGGER = LoggerFactory.getLogger("nitroclient");
	private static final Path CACHE_DIR = FabricLoader.getInstance().getConfigDir().resolve("nitro-spotify-cache");
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(6))
			.build();

	private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "nitro-spotify-art");
		t.setDaemon(true);
		return t;
	});
	private final Map<String, Identifier> ready = new ConcurrentHashMap<>();
	private final Map<String, AtomicBoolean> inflight = new ConcurrentHashMap<>();

	public Identifier getOrRequest(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}
		Identifier existing = ready.get(url);
		if (existing != null) {
			return existing;
		}
		AtomicBoolean flag = inflight.computeIfAbsent(url, k -> new AtomicBoolean(false));
		if (flag.compareAndSet(false, true)) {
			worker.execute(() -> download(url));
		}
		return null;
	}

	public void shutdown() {
		worker.shutdownNow();
	}

	private void download(String url) {
		try {
			String hash = sha1(url);
			Path file = CACHE_DIR.resolve(hash + ".img");
			byte[] bytes;
			if (Files.exists(file) && Files.size(file) > 0) {
				bytes = Files.readAllBytes(file);
			} else {
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.timeout(Duration.ofSeconds(10))
						.GET()
						.build();
				HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
				if (response.statusCode() >= 400 || response.body() == null || response.body().length == 0) {
					return;
				}
				bytes = response.body();
				Files.createDirectories(CACHE_DIR);
				Files.write(file, bytes);
			}
			byte[] imageBytes = bytes;
			MinecraftClient client = MinecraftClient.getInstance();
			if (client == null) {
				return;
			}
			client.execute(() -> register(url, hash, imageBytes));
		} catch (Exception e) {
			LOGGER.debug("Spotify art download failed: {}", e.toString());
		} finally {
			inflight.remove(url);
		}
	}

	private void register(String url, String hash, byte[] bytes) {
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			NativeImage image = NativeImage.read(in);
			Identifier id = Identifier.of("nitro", "spotify_art/" + hash);
			NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "spotify-" + hash, image);
			MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
			ready.put(url, id);
		} catch (Exception e) {
			LOGGER.debug("Spotify art register failed: {}", e.toString());
		}
	}

	private static String sha1(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		} catch (Exception e) {
			return Integer.toHexString(value.hashCode());
		}
	}
}
