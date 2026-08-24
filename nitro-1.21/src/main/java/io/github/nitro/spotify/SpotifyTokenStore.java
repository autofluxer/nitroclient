package io.github.nitro.spotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persists OAuth tokens only (never passwords). */
public final class SpotifyTokenStore {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nitro-spotify-tokens.json");

	public String accessToken = "";
	public String refreshToken = "";
	public long expiresAtEpochMs = 0L;
	public String tokenType = "Bearer";
	public String scope = "";

	public static SpotifyTokenStore load() {
		if (!Files.exists(PATH)) {
			return new SpotifyTokenStore();
		}
		try (Reader reader = Files.newBufferedReader(PATH)) {
			SpotifyTokenStore loaded = GSON.fromJson(reader, SpotifyTokenStore.class);
			return loaded != null ? loaded : new SpotifyTokenStore();
		} catch (IOException ignored) {
			return new SpotifyTokenStore();
		}
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public void clear() {
		accessToken = "";
		refreshToken = "";
		expiresAtEpochMs = 0L;
		tokenType = "Bearer";
		scope = "";
		try {
			Files.deleteIfExists(PATH);
		} catch (IOException ignored) {
		}
	}

	public boolean hasRefreshToken() {
		return refreshToken != null && !refreshToken.isBlank();
	}

	public boolean accessExpired(long skewMs) {
		return accessToken == null || accessToken.isBlank()
				|| System.currentTimeMillis() + skewMs >= expiresAtEpochMs;
	}
}
