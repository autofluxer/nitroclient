package io.github.nitro.ui.background;

import io.github.nitro.ui.NitroUiDraw;
import io.github.nitro.ui.theme.BackgroundEffect;
import io.github.nitro.ui.theme.MenuThemeAnimator;
import io.github.nitro.ui.theme.MenuThemes;
import io.github.nitro.ui.theme.NitroTheme;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class ThemeBackgroundEngine {

	private static final int MAX_PARTICLES = 72;
	private static final List<BackgroundParticle> POOL = new ArrayList<>();
	private static float[] skyPalette = defaultSky();
	private static BackgroundEffect effect = BackgroundEffect.CRIMSON_EMBERS;
	private static float blurIntensity = 0.75F;

	static {
		for (int i = 0; i < MAX_PARTICLES; i++) {
			POOL.add(new BackgroundParticle());
		}
	}

	private ThemeBackgroundEngine() {
	}

	private static float[] defaultSky() {
		return new float[] { 0.08F, 0.02F, 0.04F, 0.10F, 0.03F, 0.05F, 0.14F, 0.04F, 0.06F, 0.12F, 0.03F, 0.05F };
	}

	public static void setSkyPalette(float[] palette) {
		if (palette != null && palette.length >= 12) {
			System.arraycopy(palette, 0, skyPalette, 0, 12);
		}
	}

	public static void setEffect(BackgroundEffect value) {
		effect = value == null ? BackgroundEffect.CRIMSON_EMBERS : value;
	}

	public static void setBlurIntensity(float value) {
		blurIntensity = Math.max(0.2F, Math.min(1F, value));
	}

	public static float blurIntensity() {
		return blurIntensity;
	}

	public static void render(DrawContext context, int width, int height, float delta, int mouseX, int mouseY) {
		boolean video = io.github.nitro.video.NitroVideoEdition.active();
		float phase = MenuThemeAnimator.ambientPhase();
		float parallaxX = (mouseX - width * 0.5F) / Math.max(1, width) * (video ? 26F : 18F);
		float parallaxY = (mouseY - height * 0.5F) / Math.max(1, height) * (video ? 18F : 12F);

		int top = sample(9, 10, 11);
		int bottom = sample(0, 1, 2);
		context.fillGradient(0, 0, width, height, top, bottom);
		int veil = NitroUiDraw.withAlpha(NitroTheme.background(), (int) (0xD0 + blurIntensity * 0x28));
		context.fillGradient(0, 0, width, height, veil, 0xE8000000);

		drawAmbientGlow(context, width, height, phase, parallaxX, parallaxY);
		tickAndDrawParticles(context, width, height, delta);
		drawEffectOverlay(context, width, height, phase);
		context.fillGradient(0, height - 96, width, height, 0x00000000, 0xCC000000);
		NitroUiDraw.vignette(context, width, height, 0x88000000);
	}

	private static void drawAmbientGlow(DrawContext context, int width, int height, float phase, float px, float py) {
		float t = (System.currentTimeMillis() % 20000L) / 20000F;
		int cx = (int) (width * (0.5F + 0.08F * (float) Math.sin(t * Math.PI * 2)) + px);
		int cy = (int) (height * (0.38F + 0.05F * (float) Math.cos(t * Math.PI * 2)) + py);
		NitroUiDraw.softGlow(context, cx, cy, 420, NitroTheme.accent());
		NitroUiDraw.softGlow(context, (int) (width * 0.82F - px * 0.3F), (int) (height * 0.72F), 280, NitroTheme.secondaryAccent());
		int pulse = (int) (0x18 + 0x18 * (Math.sin(phase * 1.4F) * 0.5 + 0.5));
		NitroUiDraw.softGlow(context, (int) (width * 0.12F), (int) (height * 0.18F), 220,
				NitroUiDraw.withAlpha(NitroTheme.accent(), pulse));
	}

	private static void tickAndDrawParticles(DrawContext context, int width, int height, float delta) {
		BackgroundEffect active = MenuThemes.currentEffect();
		int target = particleBudget(active);
		int alive = 0;
		for (BackgroundParticle particle : POOL) {
			if (particle.life > 0F && particle.tick(width, height, delta)) {
				alive++;
				drawParticle(context, particle);
			}
		}
		while (alive < target && alive < MAX_PARTICLES) {
			for (BackgroundParticle particle : POOL) {
				if (particle.life <= 0F) {
					spawn(particle, width, height, active);
					alive++;
					break;
				}
			}
			if (alive >= target) {
				break;
			}
			boolean spawned = false;
			for (BackgroundParticle particle : POOL) {
				if (particle.life <= 0F) {
					spawn(particle, width, height, active);
					spawned = true;
					break;
				}
			}
			if (!spawned) {
				break;
			}
		}
	}

	private static int particleBudget(BackgroundEffect active) {
		boolean activeWindow = io.github.nitro.client.NitroClientActivity.shouldAnimateMenus();
		int budget = switch (active) {
			case GALAXY, AURORA, END_PORTAL -> 40;
			case CHERRY_PETALS, FROST_SNOW, NETHER_ASH -> 32;
			case DEEP_DARK, RAIN -> 24;
			default -> 22;
		};
		return activeWindow ? budget : Math.min(8, budget);
	}

	private static void spawn(BackgroundParticle particle, float width, float height, BackgroundEffect active) {
		int color = NitroTheme.accent();
		int type = 0;
		float speed = 0.35F;
		switch (active) {
			case END_PORTAL -> {
				color = NitroUiDraw.withAlpha(0xFFAA66FF, 0xAA);
				type = 2;
				speed = 0.25F;
			}
			case DEEP_DARK -> {
				color = NitroUiDraw.withAlpha(0xFF0AE8DA, 0x66);
				speed = 0.15F;
			}
			case NETHER_ASH, INFERNO -> {
				color = NitroUiDraw.withAlpha(0xFFFF6B35, 0x99);
				type = 1;
				speed = 0.45F;
			}
			case CHERRY_PETALS -> {
				color = NitroUiDraw.withAlpha(0xFFFFB7D5, 0xBB);
				type = 1;
				speed = 0.2F;
			}
			case AURORA -> {
				color = NitroUiDraw.withAlpha(0xFF5CFFB1, 0x88);
				speed = 0.18F;
			}
			case CYBER_GRID -> {
				color = NitroUiDraw.withAlpha(0xFF00F0FF, 0x77);
				speed = 0.55F;
			}
			case GALAXY -> {
				color = NitroUiDraw.withAlpha(0xFFE8E8FF, 0xCC);
				speed = 0.08F;
			}
			case FROST_SNOW, RAIN -> {
				color = NitroUiDraw.withAlpha(0xFFE8F4FF, 0xAA);
				type = 1;
				speed = 0.55F;
			}
			case CRIMSON_EMBERS -> {
				color = NitroUiDraw.withAlpha(NitroTheme.accent(), 0xBB);
				type = 1;
				speed = 0.3F;
			}
		}
		particle.reset(width, height, color, type, speed);
	}

	private static void drawParticle(DrawContext context, BackgroundParticle particle) {
		int alpha = (int) (particle.alpha() * 255F);
		int color = (particle.color & 0x00FFFFFF) | (alpha << 24);
		int size = Math.max(1, (int) particle.size);
		int x = (int) particle.x;
		int y = (int) particle.y;
		context.fill(x, y, x + size, y + size, color);
		// Soft glow is expensive (full gradient fills) — only for larger particles, sparsely.
		if (size > 3 && ((x + y) & 3) == 0) {
			NitroUiDraw.softGlow(context, x, y, size * 3, NitroUiDraw.withAlpha(color, alpha / 4));
		}
	}

	private static void drawEffectOverlay(DrawContext context, int width, int height, float phase) {
		BackgroundEffect active = MenuThemes.currentEffect();
		switch (active) {
			case CYBER_GRID -> drawCyberGrid(context, width, height, phase);
			case AURORA -> context.fillGradient(0, (int) (height * 0.15F), width, (int) (height * 0.45F),
					NitroUiDraw.withAlpha(0xFF00FFAA, 0x08), 0x00000000);
			case END_PORTAL -> {
				int ring = NitroUiDraw.withAlpha(0xFF6B2FFF, (int) (0x10 + 0x08 * (Math.sin(phase) * 0.5 + 0.5)));
				NitroUiDraw.strokeRoundRect(context, width / 2 - 90, height / 2 - 90, 180, 180, 90, ring);
			}
			case DEEP_DARK -> context.fill(0, 0, width, height, NitroUiDraw.withAlpha(0xFF001A14, 0x18));
			default -> {
			}
		}
	}

	private static void drawCyberGrid(DrawContext context, int width, int height, float phase) {
		int line = NitroUiDraw.withAlpha(NitroTheme.accent(), 0x18);
		int offset = (int) ((phase * 40F) % 32F);
		for (int x = -offset; x < width; x += 32) {
			context.fill(x, 0, x + 1, height, line);
		}
		for (int y = -offset; y < height; y += 32) {
			context.fill(0, y, width, y + 1, line);
		}
	}

	private static int sample(int ri, int gi, int bi) {
		float scale = 0.22F;
		return color(skyPalette[ri] * scale, skyPalette[gi] * scale, skyPalette[bi] * scale);
	}

	private static int color(float r, float g, float b) {
		return 0xFF000000 | ((int) (r * 255F) << 16) | ((int) (g * 255F) << 8) | (int) (b * 255F);
	}
}
