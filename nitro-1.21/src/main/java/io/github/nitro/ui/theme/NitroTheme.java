package io.github.nitro.ui.theme;

public final class NitroTheme {

	private static int background = 0xFF0B0D10;
	private static int surface = 0xFF12151A;
	/** Soft light blue — not neon, not orange. */
	private static int accent = 0xFF74A8D4;
	private static int secondaryAccent = 0xFF8FBBDD;
	private static int accentHover = 0xFF8FBBDD;
	private static int foreground = 0xFFFFFFFF;
	private static int muted = 0xFF8B95A8;
	private static int button = 0xD014161C;
	private static int buttonHover = 0xE01A1E26;
	private static int danger = 0xFFFF4D6A;
	private static int dangerHover = 0xFFFF6B85;
	private static int panelGlass = 0xC812151A;
	private static int panelBorder = 0x5574A8D4;
	private static int rowGlass = 0x0DFFFFFF;
	private static int rowGlassHover = 0x18FFFFFF;
	private static int success = 0xFF4ADE80;
	private static int warning = 0xFFFFD166;

	private NitroTheme() {
	}

	public static void setPalette(int bg, int surf, int acc, int fg) {
		setPalette(bg, surf, acc, lighten(acc, 0.12F), fg);
	}

	public static void setPalette(int bg, int surf, int acc, int secondary, int fg) {
		background = bg;
		surface = surf;
		accent = acc;
		secondaryAccent = secondary;
		foreground = fg;
		accentHover = lighten(acc, 0.14F);
		button = darken(surf, 0.04F);
		buttonHover = lighten(button, 0.08F);
		muted = blend(fg, 0.42F);
		panelGlass = (0xC8 << 24) | (darken(surf, 0.01F) & 0xFFFFFF);
		panelBorder = (0x55 << 24) | (acc & 0xFFFFFF);
		rowGlass = (0x0D << 24) | 0xFFFFFF;
		rowGlassHover = (0x18 << 24) | 0xFFFFFF;
	}

	public static int background() {
		return background;
	}

	public static int surface() {
		return surface;
	}

	public static int accent() {
		return accent;
	}

	public static int secondaryAccent() {
		return secondaryAccent;
	}

	public static int accentHover() {
		return accentHover;
	}

	public static int foreground() {
		return foreground;
	}

	public static int muted() {
		return muted;
	}

	public static int button() {
		return button;
	}

	public static int buttonHover() {
		return buttonHover;
	}

	public static int danger() {
		return danger;
	}

	public static int dangerHover() {
		return dangerHover;
	}

	public static int panelGlass() {
		return panelGlass;
	}

	public static int panelBorder() {
		return panelBorder;
	}

	public static int rowGlass() {
		return rowGlass;
	}

	public static int rowGlassHover() {
		return rowGlassHover;
	}

	public static int success() {
		return success;
	}

	public static int warning() {
		return warning;
	}

	private static int lighten(int color, float amount) {
		int a = (color >> 24) & 0xFF;
		int r = Math.min(255, (int) (((color >> 16) & 0xFF) * (1F + amount)));
		int g = Math.min(255, (int) (((color >> 8) & 0xFF) * (1F + amount)));
		int b = Math.min(255, (int) ((color & 0xFF) * (1F + amount)));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int darken(int color, float amount) {
		int a = (color >> 24) & 0xFF;
		int r = (int) (((color >> 16) & 0xFF) * (1F - amount));
		int g = (int) (((color >> 8) & 0xFF) * (1F - amount));
		int b = (int) ((color & 0xFF) * (1F - amount));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int blend(int color, float towardWhite) {
		int r = (int) (((color >> 16) & 0xFF) * (1F - towardWhite) + 255 * towardWhite);
		int g = (int) (((color >> 8) & 0xFF) * (1F - towardWhite) + 255 * towardWhite);
		int b = (int) ((color & 0xFF) * (1F - towardWhite) + 255 * towardWhite);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
}
