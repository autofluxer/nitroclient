package io.github.nitro.ui.theme;

import io.github.nitro.config.NitroConfig;
import io.github.nitro.ui.background.ThemeBackgroundEngine;

public final class MenuThemePreset {

	private final String id;
	private final String nameKey;
	private final int background;
	private final int surface;
	private final int accent;
	private final int secondaryAccent;
	private final int foreground;
	private final float[] sky;
	private final BackgroundEffect effect;
	private final float blurIntensity;

	public MenuThemePreset(String id, String nameKey, int background, int surface, int accent, int secondaryAccent,
			int foreground, float[] sky, BackgroundEffect effect, float blurIntensity) {
		this.id = id;
		this.nameKey = nameKey;
		this.background = background;
		this.surface = surface;
		this.accent = accent;
		this.secondaryAccent = secondaryAccent;
		this.foreground = foreground;
		this.sky = sky;
		this.effect = effect;
		this.blurIntensity = blurIntensity;
	}

	public String getId() {
		return id;
	}

	public String getNameKey() {
		return nameKey;
	}

	public int background() {
		return background;
	}

	public int surface() {
		return surface;
	}

	public int accent() {
		return accent;
	}

	public int secondaryAccent() {
		return secondaryAccent;
	}

	public int foreground() {
		return foreground;
	}

	public float[] sky() {
		return sky;
	}

	public BackgroundEffect effect() {
		return effect;
	}

	public float blurIntensity() {
		return blurIntensity;
	}

	public int getAccent() {
		return accent;
	}

	public void apply() {
		MenuThemeAnimator.transitionTo(this);
		NitroConfig.INSTANCE.menuTheme = id;
		NitroConfig.save();
	}

	public void applyInstant() {
		MenuThemeAnimator.snap(this);
		NitroConfig.INSTANCE.menuTheme = id;
		NitroConfig.save();
	}

	public void applyRuntime() {
		NitroTheme.setPalette(background, surface, accent, secondaryAccent, foreground);
		ThemeBackgroundEngine.setSkyPalette(sky);
		ThemeBackgroundEngine.setEffect(effect);
		ThemeBackgroundEngine.setBlurIntensity(blurIntensity);
	}

	public static float[] sky(float blR, float blG, float blB, float brR, float brG, float brB,
			float trR, float trG, float trB, float tlR, float tlG, float tlB) {
		return new float[] { blR, blG, blB, brR, brG, brB, trR, trG, trB, tlR, tlG, tlB };
	}
}
