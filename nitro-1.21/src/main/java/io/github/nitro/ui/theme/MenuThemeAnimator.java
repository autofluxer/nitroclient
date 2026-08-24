package io.github.nitro.ui.theme;

import io.github.nitro.ui.background.ThemeBackgroundEngine;

public final class MenuThemeAnimator {

	private static final float SNAP_THRESHOLD = 0.985F;
	private static final float LERP_SPEED = 0.09F;

	private static int fromBg;
	private static int fromSurface;
	private static int fromAccent;
	private static int fromSecondary;
	private static int fromFg;
	private static final float[] fromSky = new float[12];

	private static int toBg;
	private static int toSurface;
	private static int toAccent;
	private static int toSecondary;
	private static int toFg;
	private static final float[] toSky = new float[12];

	private static final float[] currentSky = new float[12];
	private static float progress = 1F;
	private static boolean animating;
	private static float ambientPhase;
	private static MenuThemePreset activePreset;

	private MenuThemeAnimator() {
	}

	public static void snap(MenuThemePreset preset) {
		if (preset == null) {
			return;
		}
		activePreset = preset;
		fromBg = toBg = preset.background();
		fromSurface = toSurface = preset.surface();
		fromAccent = toAccent = preset.accent();
		fromSecondary = toSecondary = preset.secondaryAccent();
		fromFg = toFg = preset.foreground();
		copySky(preset.sky(), currentSky);
		copySky(preset.sky(), fromSky);
		copySky(preset.sky(), toSky);
		progress = 1F;
		animating = false;
		applyCurrent();
	}

	public static void transitionTo(MenuThemePreset preset) {
		if (preset == null) {
			return;
		}
		activePreset = preset;
		if (!animating && progress >= 1F) {
			fromBg = NitroTheme.background();
			fromSurface = NitroTheme.surface();
			fromAccent = NitroTheme.accent();
			fromSecondary = NitroTheme.secondaryAccent();
			fromFg = NitroTheme.foreground();
			copySky(currentSky, fromSky);
		}
		toBg = preset.background();
		toSurface = preset.surface();
		toAccent = preset.accent();
		toSecondary = preset.secondaryAccent();
		toFg = preset.foreground();
		copySky(preset.sky(), toSky);
		progress = 0F;
		animating = true;
	}

	public static void tick() {
		float step = io.github.nitro.video.NitroVideoEdition.active() ? 0.022F : 0.014F;
		ambientPhase += step;
		if (!animating) {
			return;
		}
		progress = Math.min(1F, progress + LERP_SPEED);
		applyCurrent();
		if (progress >= SNAP_THRESHOLD) {
			progress = 1F;
			animating = false;
			if (activePreset != null) {
				snap(activePreset);
			}
		}
	}

	public static boolean isAnimating() {
		return animating;
	}

	public static float ambientPhase() {
		return ambientPhase;
	}

	private static void applyCurrent() {
		float t = ease(progress);
		int bg = lerpColor(fromBg, toBg, t);
		int surface = lerpColor(fromSurface, toSurface, t);
		int accent = lerpColor(fromAccent, toAccent, t);
		int secondary = lerpColor(fromSecondary, toSecondary, t);
		int fg = lerpColor(fromFg, toFg, t);
		float[] sky = new float[12];
		for (int i = 0; i < 12; i++) {
			sky[i] = fromSky[i] + (toSky[i] - fromSky[i]) * t;
		}
		NitroTheme.setPalette(bg, surface, accent, secondary, fg);
		ThemeBackgroundEngine.setSkyPalette(sky);
		copySky(sky, currentSky);
		if (activePreset != null && progress >= 1F) {
			ThemeBackgroundEngine.setEffect(activePreset.effect());
			ThemeBackgroundEngine.setBlurIntensity(activePreset.blurIntensity());
		}
	}

	private static float ease(float t) {
		return t * t * (3F - 2F * t);
	}

	private static int lerpColor(int from, int to, float t) {
		int af = (from >> 24) & 0xFF;
		int at = (to >> 24) & 0xFF;
		int rf = (from >> 16) & 0xFF;
		int rt = (to >> 16) & 0xFF;
		int gf = (from >> 8) & 0xFF;
		int gt = (to >> 8) & 0xFF;
		int bf = from & 0xFF;
		int bt = to & 0xFF;
		int a = (int) (af + (at - af) * t);
		int r = (int) (rf + (rt - rf) * t);
		int g = (int) (gf + (gt - gf) * t);
		int b = (int) (bf + (bt - bf) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static void copySky(float[] source, float[] dest) {
		System.arraycopy(source, 0, dest, 0, Math.min(12, source.length));
	}
}
