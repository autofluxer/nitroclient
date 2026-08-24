package io.github.nitro.ui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class NitroDraw {

	public static final Identifier LOGO = Identifier.of("nitro", "textures/gui/nitro_logo.png");
	/** Replaceable title-screen logo. Drop a PNG at this path to swap branding. */
	public static final Identifier TITLE_LOGO = Identifier.of("nitroclient", "textures/gui/logo.png");
	public static final Identifier BG_MENU = Identifier.of("nitro", "textures/gui/bg_menu.png");
	public static final Identifier BG_JUNGLE_1 = Identifier.of("nitro", "textures/gui/bg_jungle_1.png");
	public static final Identifier BG_JUNGLE_2 = Identifier.of("nitro", "textures/gui/bg_jungle_2.png");

	private NitroDraw() {
	}

	/**
	 * Stretch the full texture into a square. Passing the on-screen size as texture size
	 * maps UVs 0..1 across the quad (required on 1.21.11 DrawContext).
	 */
	public static void blit(DrawContext context, Identifier texture, int x, int y, int size) {
		blit(context, texture, x, y, size, size, 1F);
	}

	public static void blit(DrawContext context, Identifier texture, int x, int y, int size, float alpha) {
		blit(context, texture, x, y, size, size, alpha);
	}

	public static void blit(DrawContext context, Identifier texture, int x, int y, int w, int h) {
		blit(context, texture, x, y, w, h, 1F);
	}

	public static void blit(DrawContext context, Identifier texture, int x, int y, int w, int h, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(255F * alpha)));
		int color = (a << 24) | 0x00FFFFFF;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0F, 0F, w, h, w, h, color);
	}

	/** Full-bleed cover background (16:9 assumed). */
	public static void drawCoverBackground(DrawContext context, Identifier texture, int width, int height) {
		drawCoverBackground(context, texture, width, height, 1F, 16F / 9F);
	}

	public static void drawCoverBackground(DrawContext context, Identifier texture, int width, int height, float zoom) {
		drawCoverBackground(context, texture, width, height, zoom, 16F / 9F);
	}

	/**
	 * Cover-fit blit. {@code texAspect} only controls destination crop math;
	 * UVs always map the full texture onto the cover rect (GPU-friendly stretch).
	 */
	public static void drawCoverBackground(DrawContext context, Identifier texture, int width, int height,
			float zoom, float texAspect) {
		zoom = Math.max(1F, zoom);
		float aspect = texAspect <= 0.01F ? (16F / 9F) : texAspect;
		float screenAspect = width / (float) Math.max(1, height);
		int drawW;
		int drawH;
		if (screenAspect > aspect) {
			drawW = Math.round(width * zoom);
			drawH = Math.max(1, Math.round(drawW / aspect));
		} else {
			drawH = Math.round(height * zoom);
			drawW = Math.max(1, Math.round(drawH * aspect));
		}
		int x = (width - drawW) / 2;
		int y = (height - drawH) / 2;
		// texture size == draw size → UV 0..1 across the quad
		context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0F, 0F, drawW, drawH, drawW, drawH);
	}

	/** Overload kept for callers that pass encoded pixel size. */
	public static void drawCoverBackground(DrawContext context, Identifier texture, int width, int height,
			float zoom, int textureWidth, int textureHeight) {
		float aspect = textureWidth / (float) Math.max(1, textureHeight);
		drawCoverBackground(context, texture, width, height, zoom, aspect);
	}
}
