package io.github.nitro.ui.theme;

import io.github.nitro.config.NitroConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single Nitro theme: light-blue accents on dark-blue surfaces.
 */
public final class MenuThemes {

	private static final List<MenuThemePreset> PRESETS = new ArrayList<>();
	private static final Map<String, MenuThemePreset> BY_ID = new LinkedHashMap<>();

	static {
		register("nitro", "nitro.menu_theme.nitro",
				0xFF070B14, 0xFF10182A, 0xFF74A8D4, 0xFFA8D0F0, 0xFFFFFFFF,
				MenuThemePreset.sky(0.03F, 0.05F, 0.10F, 0.04F, 0.07F, 0.14F, 0.06F, 0.10F, 0.18F, 0.05F, 0.08F, 0.14F),
				BackgroundEffect.NITRO_BLUE, 0.28F);
	}

	private MenuThemes() {
	}

	private static void register(String id, String nameKey, int bg, int surface, int accent, int secondary, int fg,
			float[] sky, BackgroundEffect effect, float blur) {
		MenuThemePreset preset = new MenuThemePreset(id, nameKey, bg, surface, accent, secondary, fg, sky, effect, blur);
		PRESETS.add(preset);
		BY_ID.put(id, preset);
	}

	public static List<MenuThemePreset> all() {
		return Collections.unmodifiableList(PRESETS);
	}

	public static String currentId() {
		return "nitro";
	}

	public static MenuThemePreset current() {
		return BY_ID.get("nitro");
	}

	public static BackgroundEffect currentEffect() {
		MenuThemePreset preset = current();
		return preset != null ? preset.effect() : BackgroundEffect.NITRO_BLUE;
	}

	public static void apply(String id) {
		MenuThemePreset preset = BY_ID.get("nitro");
		if (preset != null) {
			preset.apply();
		}
	}

	public static void applySaved() {
		MenuThemePreset preset = BY_ID.get("nitro");
		if (preset != null) {
			preset.applyInstant();
		}
		NitroConfig.INSTANCE.menuTheme = "nitro";
	}

	public static MenuThemePreset get(String id) {
		return BY_ID.getOrDefault(id, BY_ID.get("nitro"));
	}

	public static MenuThemePreset random() {
		return BY_ID.get("nitro");
	}
}
