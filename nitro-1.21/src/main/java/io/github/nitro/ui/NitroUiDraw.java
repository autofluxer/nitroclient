package io.github.nitro.ui;

import io.github.nitro.ui.animation.NitroEasing;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;

public final class NitroUiDraw {

	private NitroUiDraw() {
	}

	public static void fillRoundRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
		if (w <= 0 || h <= 0) {
			return;
		}
		if (radius <= 0) {
			context.fill(x, y, x + w, y + h, color);
			return;
		}
		radius = Math.min(radius, Math.min(w, h) / 2);
		context.fill(x + radius, y, x + w - radius, y + h, color);
		context.fill(x, y + radius, x + w, y + h - radius, color);
		for (int dy = 0; dy < radius; dy++) {
			int inset = roundInset(radius, dy);
			context.fill(x + radius - inset, y + dy, x + radius, y + dy + 1, color);
			context.fill(x + w - radius, y + dy, x + w - radius + inset, y + dy + 1, color);
			context.fill(x + radius - inset, y + h - dy - 1, x + radius, y + h - dy, color);
			context.fill(x + w - radius, y + h - dy - 1, x + w - radius + inset, y + h - dy, color);
		}
	}

	/** Full rounded outline (edges + corner arcs). */
	public static void strokeRoundRect(DrawContext context, int x, int y, int w, int h, int radius, int color) {
		if (w <= 0 || h <= 0) {
			return;
		}
		if (radius <= 0) {
			context.fill(x, y, x + w, y + 1, color);
			context.fill(x, y + h - 1, x + w, y + h, color);
			context.fill(x, y, x + 1, y + h, color);
			context.fill(x + w - 1, y, x + w, y + h, color);
			return;
		}
		radius = Math.min(radius, Math.min(w, h) / 2);
		context.fill(x + radius, y, x + w - radius, y + 1, color);
		context.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
		context.fill(x, y + radius, x + 1, y + h - radius, color);
		context.fill(x + w - 1, y + radius, x + w, y + h - radius, color);
		for (int dy = 0; dy < radius; dy++) {
			int inset = roundInset(radius, dy);
			context.fill(x + radius - inset, y + dy, x + radius - inset + 1, y + dy + 1, color);
			context.fill(x + w - radius + inset - 1, y + dy, x + w - radius + inset, y + dy + 1, color);
			context.fill(x + radius - inset, y + h - dy - 1, x + radius - inset + 1, y + h - dy, color);
			context.fill(x + w - radius + inset - 1, y + h - dy - 1, x + w - radius + inset, y + h - dy, color);
		}
	}

	private static int roundInset(int radius, int dy) {
		return (int) Math.floor(Math.sqrt((double) radius * radius - (double) (radius - dy) * (radius - dy)));
	}

	public static void glassPanel(DrawContext context, int x, int y, int w, int h) {
		premiumPanel(context, x, y, w, h, 12);
	}

	public static void glassPanel(DrawContext context, int x, int y, int w, int h, int radius) {
		premiumPanel(context, x, y, w, h, radius);
	}

	public static void premiumPanel(DrawContext context, int x, int y, int w, int h, int radius) {
		fillRoundRect(context, x + 1, y + 2, w, h, radius, withAlpha(0x000000, 0x40));
		fillRoundRect(context, x, y, w, h, radius, NitroTheme.panelGlass());
		strokeRoundRect(context, x, y, w, h, radius, NitroTheme.panelBorder());
		strokeRoundRect(context, x, y, w, h, radius, 0x18FFFFFF);
	}

	/** Dark glass with drop shadow, top specular sheen, and a thin accent halo. */
	public static void glossyPanel(DrawContext context, int x, int y, int w, int h, int radius) {
		if (w <= 0 || h <= 0) {
			return;
		}
		fillRoundRect(context, x + 3, y + 6, w, h, radius, 0x70000000);
		fillVGradientRoundRect(context, x, y, w, h, radius, 0xE41C2028, 0xF00A0C10);
		topSheen(context, x, y, w, h, radius, 0.72F);
		strokeRoundRect(context, x - 1, y - 1, w + 2, h + 2, radius + 1, withAlpha(NitroTheme.accent(), 0x36));
		strokeRoundRect(context, x, y, w, h, radius, 0x2AFFFFFF);
		strokeRoundRect(context, x + 1, y + 1, w - 2, h - 2, Math.max(1, radius - 1), 0x14FFFFFF);
	}

	/** Bright strip along the top of a rounded rect (wet-glass highlight). */
	public static void topSheen(DrawContext context, int x, int y, int w, int h, int radius, float intensity) {
		if (w <= 2 || h <= 2 || intensity <= 0.01F) {
			return;
		}
		int sheenH = Math.max(5, Math.min(16, h / 2));
		int alpha = Math.min(0x55, Math.max(1, Math.round(0x48 * intensity)));
		fillVGradientRoundRect(context, x + 2, y + 1, w - 4, sheenH, Math.max(1, radius - 1),
				withAlpha(0xFFFFFF, alpha), 0x00FFFFFF);
	}

	public static void outerGlow(DrawContext context, int x, int y, int w, int h, int radius, int color, int spread) {
		for (int i = spread; i >= 1; i--) {
			int a = Math.max(8, ((color >>> 24) * (spread - i + 1)) / (spread + 2));
			strokeRoundRect(context, x - i, y - i, w + i * 2, h + i * 2, radius + i, withAlpha(color, a));
		}
	}

	/** Inner content well inside a premium panel. */
	public static void contentWell(DrawContext context, int x, int y, int w, int h, int radius) {
		fillRoundRect(context, x, y, w, h, radius, withAlpha(NitroTheme.surface(), 0xE6));
		strokeRoundRect(context, x, y, w, h, radius, 0x14FFFFFF);
	}

	/** Compact in-game HUD chip — dark glass + thin blue edge (Lunar-like, not oversized). */
	public static void hudGlassChip(DrawContext context, int x, int y, int w, int h) {
		hudGlassChip(context, x, y, w, h, false);
	}

	public static void hudGlassChip(DrawContext context, int x, int y, int w, int h, boolean accentFill) {
		if (w <= 0 || h <= 0) {
			return;
		}
		int radius = Math.min(4, Math.min(w, h) / 2);
		int fill = accentFill
				? withAlpha(NitroTheme.accent(), 0x55)
				: withAlpha(NitroTheme.surface(), 0x99);
		fillRoundRect(context, x, y, w, h, radius, fill);
		strokeRoundRect(context, x, y, w, h, radius, withAlpha(NitroTheme.accent(), accentFill ? 0x88 : 0x48));
	}

	public static void premiumHeader(DrawContext context, int x, int y, int w, String title, String subtitle) {
		var renderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
		context.drawText(renderer, net.minecraft.text.Text.literal(title), x, y, NitroTheme.foreground(), false);
		if (subtitle != null && !subtitle.isEmpty()) {
			context.drawText(renderer, net.minecraft.text.Text.literal(subtitle), x, y + 11, NitroTheme.muted(), false);
		}
	}

	public static void brandHeader(DrawContext context, int x, int y, int logoSize, String title, String subtitle) {
		NitroLogoRenderer.drawLogo(context, x, y, logoSize);
		int textX = x + logoSize + 12;
		var renderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
		context.drawText(renderer, net.minecraft.text.Text.literal(title), textX, y + 6, NitroTheme.foreground(), false);
		if (subtitle != null && !subtitle.isEmpty()) {
			context.drawText(renderer, net.minecraft.text.Text.literal(subtitle), textX, y + 18, NitroTheme.muted(), false);
		}
	}

	public static void divider(DrawContext context, int x, int y, int w) {
		context.fill(x, y, x + w, y + 1, withAlpha(0xFFFFFF, 0x18));
	}

	public static void softGlow(DrawContext context, int cx, int cy, int size, int color) {
		int half = size / 2;
		context.fillGradient(cx - half, cy - half, cx + half, cy + half,
				withAlpha(color, (color >> 24) & 0xFF), withAlpha(color, 0x00));
	}

	public static void vignette(DrawContext context, int width, int height, int color) {
		int band = Math.max(40, height / 10);
		context.fillGradient(0, 0, width, band, color, 0x00000000);
		context.fillGradient(0, height - band, width, height, 0x00000000, color);
		context.fillGradient(0, 0, band, height, color, 0x00000000);
		context.fillGradient(width - band, 0, width, height, 0x00000000, color);
	}

	public static int withAlpha(int color, int alpha) {
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	public static int hoverFill(boolean hovered, int normal, int hover) {
		return hovered ? hover : normal;
	}

	public static float animatedHover(boolean hovered, float current, float delta) {
		float target = hovered ? 1F : 0F;
		return NitroEasing.approach(current, target, Math.max(0.016F, delta), 8F);
	}

	/** Horizontal gradient inside a rounded rect (FastClient Store / accent buttons). */
	public static void fillHGradientRoundRect(DrawContext context, int x, int y, int w, int h, int radius,
			int leftColor, int rightColor) {
		if (w <= 0 || h <= 0) {
			return;
		}
		radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		for (int i = 0; i < w; i++) {
			float t = w <= 1 ? 0F : i / (float) (w - 1);
			int color = lerpColor(leftColor, rightColor, t);
			int top = 0;
			int bot = 0;
			if (radius > 0) {
				if (i < radius) {
					int dx = radius - i;
					int inset = radius - (int) Math.floor(Math.sqrt((double) radius * radius - (double) dx * dx));
					top = inset;
					bot = inset;
				} else if (i >= w - radius) {
					int dx = i - (w - radius - 1);
					int inset = radius - (int) Math.floor(Math.sqrt((double) radius * radius - (double) dx * dx));
					top = inset;
					bot = inset;
				}
			}
			context.fill(x + i, y + top, x + i + 1, y + h - bot, color);
		}
	}

	/** Vertical gradient fill for toolbar hover (darker bottom → lighter top). */
	public static void fillVGradientRoundRect(DrawContext context, int x, int y, int w, int h, int radius,
			int topColor, int bottomColor) {
		if (w <= 0 || h <= 0) {
			return;
		}
		radius = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
		for (int j = 0; j < h; j++) {
			float t = h <= 1 ? 0F : j / (float) (h - 1);
			int color = lerpColor(topColor, bottomColor, t);
			int left = 0;
			int right = 0;
			if (radius > 0) {
				if (j < radius) {
					int dy = radius - j;
					int inset = radius - (int) Math.floor(Math.sqrt((double) radius * radius - (double) dy * dy));
					left = inset;
					right = inset;
				} else if (j >= h - radius) {
					int dy = j - (h - radius - 1);
					int inset = radius - (int) Math.floor(Math.sqrt((double) radius * radius - (double) dy * dy));
					left = inset;
					right = inset;
				}
			}
			context.fill(x + left, y + j, x + w - right, y + j + 1, color);
		}
	}

	public static int lerpColor(int from, int to, float t) {
		t = Math.max(0F, Math.min(1F, t));
		int a0 = (from >> 24) & 0xFF;
		int r0 = (from >> 16) & 0xFF;
		int g0 = (from >> 8) & 0xFF;
		int b0 = from & 0xFF;
		int a1 = (to >> 24) & 0xFF;
		int r1 = (to >> 16) & 0xFF;
		int g1 = (to >> 8) & 0xFF;
		int b1 = to & 0xFF;
		int a = (int) (a0 + (a1 - a0) * t);
		int r = (int) (r0 + (r1 - r0) * t);
		int g = (int) (g0 + (g1 - g0) * t);
		int b = (int) (b0 + (b1 - b0) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
