package io.github.nitro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NitroConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("nitro-client.json");

	public boolean fancyMainMenu = true;
	public String menuTheme = "nitro";
	public String activeHudPreset = "smp";
	public boolean hudSnapGrid = true;
	/**
	 * Title-screen backdrop: {@code panorama} (Minecraft cube-map), {@code jungle},
	 * {@code menu}, or {@code video}.
	 */
	public String titleBackground = "panorama";
	public int titleButtonWidth = 200;
	public int titleButtonHeight = 20;
	public int titleButtonGap = 4;
	public int titleOverlayAlpha = 96;
	public float titleFadeSpeed = 7F;
	public float titleUiOpacity = 1F;
	/** Animated title / Click GUI video background. */
	public boolean animatedMenuBackground = true;
	/** Display FPS hint for menu background (1–60). Playback speed is primarily cinematic slowdown. */
	public int menuBgFps = 24;
	/** Hide HUD / overlays while Minecraft is minimized or unfocused. */
	public boolean pauseOverlaysWhenUnfocused = true;
	public java.util.Map<String, Boolean> moduleStates = new java.util.HashMap<>();
	public java.util.Map<String, HudElementLayout> hudLayouts = new java.util.HashMap<>();
	/** Non-secret Spotify HUD / UX preferences. Tokens live in nitro-spotify-tokens.json. */
	public SpotifyUiSettings spotify = new SpotifyUiSettings();

	public static NitroConfig INSTANCE = new NitroConfig();

	private NitroConfig() {
	}

	public static void load() {
		if (!Files.exists(CONFIG_PATH)) {
			save();
			normalizeInstance();
			return;
		}
		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			NitroConfig loaded = GSON.fromJson(reader, NitroConfig.class);
			if (loaded != null) {
				INSTANCE = loaded;
			}
		} catch (IOException ignored) {
			INSTANCE = new NitroConfig();
		}
		normalizeInstance();
	}

	private static void normalizeInstance() {
		if (INSTANCE.menuTheme == null || INSTANCE.menuTheme.isBlank()) {
			INSTANCE.menuTheme = "nitro";
		}
		if (INSTANCE.activeHudPreset == null || INSTANCE.activeHudPreset.isBlank()) {
			INSTANCE.activeHudPreset = "smp";
		}
		if (!INSTANCE.fancyMainMenu) {
			INSTANCE.fancyMainMenu = true;
		}
		if (INSTANCE.moduleStates == null) {
			INSTANCE.moduleStates = new java.util.HashMap<>();
		}
		if (INSTANCE.hudLayouts == null) {
			INSTANCE.hudLayouts = new java.util.HashMap<>();
		}
		if (INSTANCE.menuBgFps < 1 || INSTANCE.menuBgFps > 60) {
			INSTANCE.menuBgFps = 30;
		}
		if (INSTANCE.titleBackground == null || INSTANCE.titleBackground.isBlank()) {
			INSTANCE.titleBackground = "panorama";
		} else {
			INSTANCE.titleBackground = INSTANCE.titleBackground.trim().toLowerCase();
		}
		INSTANCE.titleButtonWidth = clamp(INSTANCE.titleButtonWidth, 140, 260);
		INSTANCE.titleButtonHeight = clamp(INSTANCE.titleButtonHeight, 16, 28);
		INSTANCE.titleButtonGap = clamp(INSTANCE.titleButtonGap, 2, 10);
		INSTANCE.titleOverlayAlpha = clamp(INSTANCE.titleOverlayAlpha, 0, 180);
		if (INSTANCE.titleFadeSpeed < 2F || INSTANCE.titleFadeSpeed > 16F) {
			INSTANCE.titleFadeSpeed = 7F;
		}
		if (INSTANCE.titleUiOpacity < 0.4F || INSTANCE.titleUiOpacity > 1F) {
			INSTANCE.titleUiOpacity = 1F;
		}
		if (INSTANCE.spotify == null) {
			INSTANCE.spotify = new SpotifyUiSettings();
		}
		INSTANCE.spotify.normalize();
	}

	public static void reloadTheme() {
		load();
		io.github.nitro.ui.theme.MenuThemes.applySaved();
	}

	public static void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
