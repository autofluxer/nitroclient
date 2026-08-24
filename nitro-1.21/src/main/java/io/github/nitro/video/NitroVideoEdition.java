package io.github.nitro.video;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Animated Nitro Client build detection.
 * Motion jars ship {@code /nitro-video-edition.flag}; the launcher also writes
 * {@code config/nitro/video-edition.json}. Branding stays Nitro Client.
 */
public final class NitroVideoEdition {

	private static final boolean COMPILED_VIDEO = detectCompiledFlag();
	private static Boolean cached;

	private NitroVideoEdition() {
	}

	public static boolean active() {
		if (cached != null) {
			return cached;
		}
		cached = COMPILED_VIDEO || readMarker();
		return cached;
	}

	public static boolean compiled() {
		return COMPILED_VIDEO;
	}

	private static boolean detectCompiledFlag() {
		try (InputStream in = NitroVideoEdition.class.getResourceAsStream("/nitro-video-edition.flag")) {
			return in != null;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static boolean readMarker() {
		try {
			Path path = FabricLoader.getInstance().getConfigDir().resolve("nitro").resolve("video-edition.json");
			if (!Files.exists(path)) {
				return false;
			}
			try (Reader reader = Files.newBufferedReader(path)) {
				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				return !json.has("enabled") || json.get("enabled").getAsBoolean();
			}
		} catch (Throwable ignored) {
			return false;
		}
	}
}
