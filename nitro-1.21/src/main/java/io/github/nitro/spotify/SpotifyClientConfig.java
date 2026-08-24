package io.github.nitro.spotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Public Spotify app Client ID (PKCE — no secret).
 * Resolved from {@code NITRO_SPOTIFY_CLIENT_ID}, else {@code config/nitro-spotify.json}.
 */
public final class SpotifyClientConfig {

	public static final String REDIRECT_URI = "http://127.0.0.1:43821/callback";
	public static final int CALLBACK_PORT = 43821;
	public static final String SCOPES = String.join(" ",
			"user-read-currently-playing",
			"user-read-playback-state",
			"user-modify-playback-state",
			"user-read-recently-played",
			"user-read-private",
			"playlist-read-private",
			"user-library-read");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nitro-spotify.json");

	public String clientId = "";

	public static String resolveClientId() {
		String env = System.getenv("NITRO_SPOTIFY_CLIENT_ID");
		if (env != null && !env.isBlank()) {
			return env.trim();
		}
		SpotifyClientConfig cfg = load();
		return cfg.clientId == null ? "" : cfg.clientId.trim();
	}

	public static SpotifyClientConfig load() {
		if (!Files.exists(PATH)) {
			SpotifyClientConfig fresh = new SpotifyClientConfig();
			fresh.save();
			return fresh;
		}
		try (Reader reader = Files.newBufferedReader(PATH)) {
			SpotifyClientConfig loaded = GSON.fromJson(reader, SpotifyClientConfig.class);
			return loaded != null ? loaded : new SpotifyClientConfig();
		} catch (IOException ignored) {
			return new SpotifyClientConfig();
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

	public static void saveClientId(String clientId) {
		SpotifyClientConfig cfg = load();
		cfg.clientId = clientId == null ? "" : clientId.trim();
		cfg.save();
	}

	public static Path configPath() {
		return PATH;
	}
}
