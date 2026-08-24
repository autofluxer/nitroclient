package io.github.nitro.spotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Cached Spotify account profile (non-secret). */
public final class SpotifyProfileStore {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("nitro-spotify-profile.json");

	public String displayName = "";
	public String product = "";
	public String id = "";

	public static SpotifyProfileStore load() {
		if (!Files.exists(PATH)) {
			return new SpotifyProfileStore();
		}
		try (Reader reader = Files.newBufferedReader(PATH)) {
			SpotifyProfileStore loaded = GSON.fromJson(reader, SpotifyProfileStore.class);
			return loaded != null ? loaded : new SpotifyProfileStore();
		} catch (IOException ignored) {
			return new SpotifyProfileStore();
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
		displayName = "";
		product = "";
		id = "";
		try {
			Files.deleteIfExists(PATH);
		} catch (IOException ignored) {
		}
	}

	public boolean hasName() {
		return displayName != null && !displayName.isBlank();
	}
}
