/*
 * Nitro Client - main menu colour preset
 */
package io.github.solclient.client.ui.screen;

import io.github.solclient.client.ui.Theme;
import io.github.solclient.client.util.data.Colour;
import lombok.Getter;

public final class MenuThemePreset {

	@Getter
	private final String id;
	@Getter
	private final String nameKey;
	private final Theme theme;
	private final float[] sky;

	MenuThemePreset(String id, String nameKey, Theme theme, float[] sky) {
		this.id = id;
		this.nameKey = nameKey;
		this.theme = theme;
		this.sky = sky;
	}

	public Colour accentColour() {
		return theme.accent;
	}

	public void apply() {
		Theme.setCurrent(theme.clone());
		NitroAnimatedBackground.setSkyPalette(sky);
	}

	static Theme palette(int bg, int button, int accent, int fg) {
		Theme theme = Theme.DARK.clone();
		theme.bg = c(bg);
		theme.button = c(button);
		theme.buttonHover = brighten(button, 12);
		theme.buttonSecondary = brighten(button, 20);
		theme.buttonSecondaryHover = brighten(button, 28);
		theme.fg = c(fg);
		theme.fgButton = c(fg);
		theme.fgButtonHover = brighten(fg, -15);
		theme.accent = c(accent);
		theme.accentHover = brighten(accent, 18);
		theme.accentFg = c(0xFF0A1628);
		theme.transparent1 = brighten(bg, 10);
		theme.transparent2 = brighten(bg, 18);
		theme.danger = c(0xFFFF4D6A);
		theme.dangerHover = c(0xFFFF7088);
		return theme;
	}

	static float[] sky(float blR, float blG, float blB, float brR, float brG, float brB,
			float trR, float trG, float trB, float tlR, float tlG, float tlB) {
		return new float[] { blR, blG, blB, brR, brG, brB, trR, trG, trB, tlR, tlG, tlB };
	}

	private static Colour c(int value) {
		return new Colour(value);
	}

	private static Colour brighten(int rgb, int amount) {
		int r = Math.min(255, Math.max(0, ((rgb >> 16) & 0xFF) + amount));
		int g = Math.min(255, Math.max(0, ((rgb >> 8) & 0xFF) + amount));
		int b = Math.min(255, Math.max(0, (rgb & 0xFF) + amount));
		return new Colour(0xFF000000 | (r << 16) | (g << 8) | b);
	}

}
