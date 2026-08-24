/*
 * Nitro Client - main menu theme presets
 */
package io.github.solclient.client.ui.screen;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import io.github.solclient.client.mod.impl.core.CoreMod;

public final class MenuThemes {

	private static final List<MenuThemePreset> PRESETS = new ArrayList<>();
	private static final Map<String, MenuThemePreset> BY_ID = new LinkedHashMap<>();

	static {
		register("nitro", "sol_client.menu_theme.nitro",
				MenuThemePreset.palette(0xFF0D1B2A, 0xFF152238, 0xFF3DB8FF, 0xFFE8F4FF),
				MenuThemePreset.sky(0.42F, 0.70F, 0.94F, 0.48F, 0.76F, 0.98F, 0.62F, 0.84F, 1F, 0.55F, 0.80F, 1F));

		register("crimson", "sol_client.menu_theme.crimson",
				MenuThemePreset.palette(0xFF1A0A10, 0xFF2A1420, 0xFFFF4D6A, 0xFFFFE8EE),
				MenuThemePreset.sky(0.45F, 0.22F, 0.32F, 0.52F, 0.28F, 0.38F, 0.68F, 0.35F, 0.45F, 0.58F, 0.30F, 0.42F));

		register("emerald", "sol_client.menu_theme.emerald",
				MenuThemePreset.palette(0xFF081A12, 0xFF123024, 0xFF3DFF9A, 0xFFE8FFF2),
				MenuThemePreset.sky(0.20F, 0.48F, 0.38F, 0.24F, 0.55F, 0.44F, 0.35F, 0.72F, 0.58F, 0.28F, 0.62F, 0.50F));

		register("violet", "sol_client.menu_theme.violet",
				MenuThemePreset.palette(0xFF120A1F, 0xFF1E1430, 0xFFB388FF, 0xFFF3E8FF),
				MenuThemePreset.sky(0.32F, 0.22F, 0.52F, 0.38F, 0.28F, 0.58F, 0.52F, 0.38F, 0.72F, 0.45F, 0.32F, 0.65F));

		register("sunset", "sol_client.menu_theme.sunset",
				MenuThemePreset.palette(0xFF1A1008, 0xFF2A1C12, 0xFFFF9A5C, 0xFFFFF0E8),
				MenuThemePreset.sky(0.55F, 0.32F, 0.22F, 0.62F, 0.38F, 0.28F, 0.78F, 0.48F, 0.35F, 0.70F, 0.42F, 0.30F));

		register("midnight", "sol_client.menu_theme.midnight",
				MenuThemePreset.palette(0xFF050810, 0xFF0C1420, 0xFF6B8FFF, 0xFFD8E4FF),
				MenuThemePreset.sky(0.08F, 0.12F, 0.22F, 0.10F, 0.14F, 0.26F, 0.14F, 0.18F, 0.32F, 0.12F, 0.16F, 0.28F));

		register("gold", "sol_client.menu_theme.gold",
				MenuThemePreset.palette(0xFF1A1408, 0xFF2A2210, 0xFFFFC857, 0xFFFFF8E8),
				MenuThemePreset.sky(0.48F, 0.38F, 0.18F, 0.55F, 0.44F, 0.22F, 0.72F, 0.58F, 0.32F, 0.65F, 0.50F, 0.28F));

		register("arctic", "sol_client.menu_theme.arctic",
				MenuThemePreset.palette(0xFFE8F4FF, 0xFFD0E8FA, 0xFF2196F3, 0xFF0D1B2A),
				MenuThemePreset.sky(0.82F, 0.92F, 0.98F, 0.78F, 0.90F, 0.98F, 0.90F, 0.96F, 1F, 0.86F, 0.94F, 1F));
	}

	private MenuThemes() {
	}

	private static void register(String id, String nameKey, Theme theme, float[] sky) {
		MenuThemePreset preset = new MenuThemePreset(id, nameKey, theme, sky);
		PRESETS.add(preset);
		BY_ID.put(id, preset);
	}

	public static List<MenuThemePreset> all() {
		return Collections.unmodifiableList(PRESETS);
	}

	public static String currentId() {
		if (CoreMod.instance == null || CoreMod.instance.menuTheme == null) {
			return "nitro";
		}
		return BY_ID.containsKey(CoreMod.instance.menuTheme) ? CoreMod.instance.menuTheme : "nitro";
	}

	public static void apply(String id) {
		MenuThemePreset preset = BY_ID.get(id);
		if (preset == null) {
			preset = BY_ID.get("nitro");
		}
		preset.apply();
		if (CoreMod.instance != null) {
			CoreMod.instance.menuTheme = preset.getId();
		}
	}

	public static void applySaved() {
		apply(currentId());
	}

	public static void applyRandom() {
		int index = ThreadLocalRandom.current().nextInt(PRESETS.size());
		apply(PRESETS.get(index).getId());
	}

}
